package io.gatling.tcp

import akka.actor.ActorRef
import io.gatling.core.result.writer.DataWriterClient
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.{Channel, ChannelFuture, ChannelFutureListener, Channels}
import org.scalamock.FunctionAdapter1
import org.scalatest._
import org.scalamock.scalatest._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * Created by stefancompton on 22/08/2018.
  */
class TcpEngineTest extends FlatSpec with MockFactory {

  val mockChannelFuture = stub[ChannelFuture]
  val mockChannel = mock[Channel]

  (mockChannelFuture.isSuccess _) when() returns(true)
//  (mockChannelFuture.addListener _) when(new FunctionAdapter1((x:ChannelFutureListener) => true)) returns()
  (mockChannelFuture.getChannel _) when() returns(mockChannel)

  val tcpEngine = new TcpEngine()
  val session = Session("SessionName", "SessionID")
  val tcpTx = TcpTx(session, ActorRef.noSender, TimeHelper.nowMillis, tcpProtocol, tcpMessage, "ReqName")
  val bootstrap = new ClientBootstrap(tcpEngine.socketChannelFactory)


  val listener = new MessageListener(tcpTx, ActorRef.noSender)
  val tcpProtocol = TcpProtocol("localhost", 0, DelimiterBasedTcpFramer(Array(' '), false), Some(TcpTls("TLSv1.2", "abc", "abc")))
  val tcpMessage = TextTcpMessage("Message")

  "A TCP TLS Connection" should "Have an SSL attribute" in {

    val result = tcpEngine.tcpClient(session, tcpProtocol, listener, mockChannelFuture, bootstrap)
    assert (result.isInstanceOf[Future[Session]])
    val pipeline = bootstrap.getPipelineFactory.getPipeline
    assert(pipeline.getNames.contains("ssl"))
  }

}
