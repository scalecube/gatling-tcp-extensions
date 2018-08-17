package io.gatling.tcp

import java.io.FileInputStream
import java.net.InetSocketAddress
import java.security.{KeyStore, SecureRandom}
import java.util.concurrent.{Executors, TimeUnit}

import javax.net.ssl.{KeyManagerFactory, TrustManagerFactory}
import javax.net.ssl.SSLContext
import akka.actor.ActorRef
import com.typesafe.scalalogging.StrictLogging
import io.gatling.core.akka.AkkaDefaults
import io.gatling.core.session.Session
import io.gatling.tcp.check.TcpCheck
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.jboss.netty.channel._
import org.jboss.netty.channel.socket.nio.{NioClientBossPool, NioClientSocketChannelFactory, NioWorkerPool}
import org.jboss.netty.handler.codec.frame.{DelimiterBasedFrameDecoder, FrameDecoder, LengthFieldBasedFrameDecoder, LengthFieldPrepender}
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import org.jboss.netty.handler.codec.protobuf.{ProtobufVarint32FrameDecoder, ProtobufVarint32LengthFieldPrepender}
import org.jboss.netty.handler.codec.string.{StringDecoder, StringEncoder}
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.util.{CharsetUtil, HashedWheelTimer}

import org.apache.commons.io.IOUtils

import scala.concurrent.{Future, Promise}

object TcpEngine extends AkkaDefaults with StrictLogging {
  private var _instance: Option[TcpEngine] = None

  val NO_TLS = 0


  def start(): Unit = {
    if (!_instance.isDefined) {
      val client = new TcpEngine
      _instance = Some(client)
      system.registerOnTermination(stop())
    }
  }

  def stop(): Unit = {
    _instance.map { engine =>
      engine.nioThreadPool.shutdown()
    }
    _instance = None
  }

  def instance: TcpEngine = _instance match {
    case Some(engine) => engine
    case _ => throw new UnsupportedOperationException("Tcp engine hasn't been started")
  }
}

case class TcpTx(session: Session,
                 next: ActorRef,
                 start: Long,
                 protocol: TcpProtocol,
                 message: TcpMessage,
                 requestName: String,
                 check: Option[TcpCheck] = None,
                 updates: List[Session => Session] = Nil) {
  def applyUpdates(session: Session) = {
    val newSession = session.update(updates)
    copy(session = newSession, updates = Nil)
  }
}

trait TcpFramer

case class LengthBasedTcpFramer(lengthFieldOffset: Int, lengthFieldLength: Int,
                                lengthAdjustment: Int, bytesToStrip: Int) extends TcpFramer {
  def this(lengthFieldLength: Int) = this(0, lengthFieldLength, 0, lengthFieldLength)
}

case class DelimiterBasedTcpFramer(delimiters: Array[Byte], stripDelimiter: Boolean) extends TcpFramer {
  def this(delimiters: Array[Byte]) = this(delimiters, true)
}

case object ProtobufVarint32TcpFramer extends TcpFramer

class TcpEngine {
  val numWorkers = Runtime.getRuntime.availableProcessors()
  val nettyTimer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS)
  val nioThreadPool = Executors.newCachedThreadPool
  val socketChannelFactory = new NioClientSocketChannelFactory(new NioClientBossPool(nioThreadPool, 1, nettyTimer, null), new NioWorkerPool(nioThreadPool, numWorkers))

  val stringDecoder: StringDecoder = new StringDecoder(CharsetUtil.UTF_8)

  val encoder: StringEncoder = new StringEncoder(CharsetUtil.UTF_8)

  //TODO Add TLS here
  def tcpClient(session: Session, protocol: TcpProtocol, listener: MessageListener): Future[Session] = {

    val TLS = "TLS"
    val TLS_1_2 = "TLSv1.2"

    val trustStoreResource = "certificates/gatling_truststore_lt02.jks"
    val keyStoreResource = "certificates/gatling-keystore.jks"
    val password = "password0"


    val bootstrap = new ClientBootstrap(socketChannelFactory)
    bootstrap.setPipelineFactory(new ChannelPipelineFactory {

      def getSSLHandler(): SslHandler = {
        var context = SSLContext.getInstance(TLS_1_2)
        val ks = KeyStore.getInstance(KeyStore.getDefaultType)
        ks.load(getClass.getClassLoader.getResourceAsStream(trustStoreResource), password.toCharArray)
        val tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
        tmf.init(ks)
        val trustManager = tmf.getTrustManagers
        context.init(Array(), trustManager, null)

        var engine = context.createSSLEngine()
        engine.setUseClientMode(true)
        new SslHandler(engine, false)
      }

      override def getPipeline: ChannelPipeline = {
        val pipeline = Channels.pipeline()
        val framer = resolveFramer(protocol)
        val prepender = resolvePrepender(protocol)
        if (protocol.tls > 0) {
          val handler = getSSLHandler()
          pipeline.addLast("ssl", handler)
        }
        pipeline.addLast("framer", framer)
        pipeline.addLast("prepender", prepender)
        pipeline.addLast("decoder", stringDecoder)
        pipeline.addLast("encoder", encoder)
        pipeline.addLast("listener", listener)
        pipeline
      }
    })
    val channelFuture = bootstrap.connect(new InetSocketAddress(protocol.address, protocol.port))
    val promise = Promise[Session]()
    channelFuture.addListener(new ChannelFutureListener {
      override def operationComplete(p1: ChannelFuture): Unit = {
        if (channelFuture.isSuccess) {
          val channel = channelFuture.getChannel
          session("channel").asOption[Channel] match {
            case Some(ch) => promise.trySuccess(session)
            case None => promise.trySuccess(session.set("channel", channel))
          }
        } else {
          promise.failure(p1.getCause)
        }
      }
    })
    promise.future

  }

  private def resolvePrepender(protocol: TcpProtocol): OneToOneEncoder {def encode(ctx: ChannelHandlerContext, channel: Channel, msg: Any): AnyRef} = {
    val prepender = protocol.framer match {
      case LengthBasedTcpFramer(offset, lenght, adjustment, strip) => new LengthFieldPrepender(lenght)
      case DelimiterBasedTcpFramer(delimiters, strip) => new OneToOneEncoder {
        override def encode(ctx: ChannelHandlerContext, channel: Channel, msg: scala.Any): AnyRef = {
          msg match {
            case buffer: ChannelBuffer => ChannelBuffers.copiedBuffer(msg.asInstanceOf[ChannelBuffer], ChannelBuffers.copiedBuffer(delimiters))
            case _ => ???
          }
        }
      }
      case ProtobufVarint32TcpFramer => new ProtobufVarint32LengthFieldPrepender
    }
    prepender
  }

  private def resolveFramer(protocol: TcpProtocol): FrameDecoder = {
    protocol.framer match {
      case LengthBasedTcpFramer(offset, lenght, adjustment, strip) => new LengthFieldBasedFrameDecoder(Short.MaxValue, offset, lenght, adjustment, strip)
      case DelimiterBasedTcpFramer(delimiters, strip) => new DelimiterBasedFrameDecoder(Short.MaxValue, strip, ChannelBuffers.copiedBuffer(delimiters))
      case ProtobufVarint32TcpFramer => new ProtobufVarint32FrameDecoder()
    }
  }

  def startTcpTransaction(tx: TcpTx, actor: ActorRef): Unit = {
    val listener = new MessageListener(tx, actor)
    import scala.concurrent.ExecutionContext.Implicits.global

    val client: Future[Session] = tcpClient(tx.session, tx.protocol, listener)

    client.onComplete {
      case scala.util.Success(session) => actor ! OnConnect(tx, session("channel").asOption[Channel].get, System.currentTimeMillis())
      case scala.util.Failure(th) => actor ! OnConnectFailed(tx, System.currentTimeMillis())
    }
  }
}
