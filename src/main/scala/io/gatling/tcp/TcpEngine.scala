package io.gatling.tcp

import java.net.InetSocketAddress
import java.util.concurrent.{ Executors, TimeUnit }

import akka.actor.ActorRef
import com.typesafe.scalalogging.StrictLogging
import io.gatling.core.akka.AkkaDefaults
import io.gatling.core.check.Check
import io.gatling.core.session.Session
import io.gatling.tcp.check.TcpCheck
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel._
import org.jboss.netty.channel.socket.nio.{ NioWorkerPool, NioClientBossPool, NioClientSocketChannelFactory }
import org.jboss.netty.handler.codec.frame.{ LengthFieldPrepender, LengthFieldBasedFrameDecoder }
import org.jboss.netty.handler.codec.string.{ StringEncoder, StringDecoder }
import org.jboss.netty.util.{ CharsetUtil, HashedWheelTimer }

import scala.concurrent.{Future, Promise}
import scala.util.Try

object TcpEngine extends AkkaDefaults with StrictLogging {
  private var _instance: Option[TcpEngine] = None

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
    case _            => throw new UnsupportedOperationException("Tcp engine hasn't been started")
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

class TcpEngine {
  val numWorkers = Runtime.getRuntime.availableProcessors()
  val nettyTimer = new HashedWheelTimer(10, TimeUnit.MILLISECONDS)
  val nioThreadPool = Executors.newCachedThreadPool
  val socketChannelFactory = new NioClientSocketChannelFactory(new NioClientBossPool(nioThreadPool, 1, nettyTimer, null), new NioWorkerPool(nioThreadPool, numWorkers))

  val prepender: LengthFieldPrepender = new LengthFieldPrepender(4)

  val stringDecoder: StringDecoder = new StringDecoder(CharsetUtil.UTF_8)

  val encoder: StringEncoder = new StringEncoder(CharsetUtil.UTF_8)

  def tcpClient(session: Session, protocol: TcpProtocol, listener: MessageListener) : Future[Session] = {
    val bootstrap = new ClientBootstrap(socketChannelFactory)
    bootstrap.setPipelineFactory(new ChannelPipelineFactory {
      override def getPipeline: ChannelPipeline = {
        val pipeline = Channels.pipeline()
        pipeline.addLast("framer", new LengthFieldBasedFrameDecoder(Short.MaxValue, 0, 4, 0, 4))
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
            case None     => promise.trySuccess(session.set("channel", channel))
          }
        } else {
          promise.failure(p1.getCause)
        }
      }
    })
    promise.future

  }

  def startTcpTransaction(tx: TcpTx, actor: ActorRef) : Unit = {
    val listener = new MessageListener(tx, actor)
    import scala.concurrent.ExecutionContext.Implicits.global

    val client: Future[Session] = tcpClient(tx.session, tx.protocol, listener)

    client.onComplete {
      case scala.util.Success(session) => actor ! OnConnect(tx, session("channel").asOption[Channel].get, System.currentTimeMillis())
      case scala.util.Failure(th) => actor ! OnConnectFailed(tx,System.currentTimeMillis())
    }
  }
}
