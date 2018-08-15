import akka.actor.{ActorRef, ActorSystem, Terminated}
import akka.testkit.{TestKit, TestProbe}
import io.gatling.core.akka.GatlingActorSystem
import io.gatling.core.check.Check
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.result.message.{KO, OK}
import io.gatling.core.session.Session
import io.gatling.tcp.Predef._
import io.gatling.tcp._
import io.gatling.tcp.check.TcpCheck
import org.jboss.netty.channel.Channel
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import io.gatling.tcp.TcpEngine._
import scala.concurrent.duration._

class TcpActorSpec(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers with BeforeAndAfterAll
with MockFactory {

  def this() = this(GatlingActorSystem.start())

  val protocol = tcp.address("127.0.0.1").port(4800).lengthBased(4, NO_TLS)
  val dataWriterClient = new TestDataWriterClient()

  GatlingConfiguration.setUpForTest()

  override def afterAll() {
    GatlingActorSystem.shutdown()
//    TestKit.shutdownActorSystem(system)
  }

  def connect(actor: ActorRef, channel : Channel) : Unit = {
    val txNext = TestProbe()
    val tx = TcpTx(session = Session("Scenario", "User"),
      next = txNext.ref, start = 0l, protocol = protocol,
      message = null, requestName = "Connect")
    actor ! OnConnect(tx, channel, 1l)
  }
  "Tcp actor in initial state" must {

    "save tcp actor to session and send next transaction" in {
      val tcpActor = system.actorOf(TcpActor.props(dataWriterClient))
      val txNext = TestProbe()
      val tx = TcpTx(session = Session("Scenario", "User"),
        next = txNext.ref, start = 0l, protocol = protocol,
        message = null, requestName = "Connect")
      val channel = mock[Channel]
      tcpActor ! OnConnect(tx, channel, 1l)

      val expectedSession = txNext.expectMsgClass(classOf[Session])
      expectedSession("tcpActor").as[ActorRef] shouldEqual tcpActor
      expectedSession.status shouldEqual OK
    }
    "mark session as failed and send next transaction" in {
      val tcpActor = system.actorOf(TcpActor.props(dataWriterClient))
      val txNext = TestProbe()
      val tx = TcpTx(session = Session("Scenario", "User"),
        next = txNext.ref, start = 0l, protocol = protocol,
        message = null, requestName = "Connect")
      tcpActor ! OnConnectFailed(tx, 0l)

      val expectedSession = txNext.expectMsgClass(classOf[Session])
      expectedSession.status shouldEqual KO
    }

    "stop context on unknown message" in {
      val tcpActor = system.actorOf(TcpActor.props(dataWriterClient))
      val probe = TestProbe()
      probe.watch(tcpActor)
      tcpActor ! "hello"
      probe.expectMsgClass(classOf[Terminated])
    }
  }

  "Tcp actor in connected state" must {

    "send message to channel and proceed next transaction without check" in {
      val tcpActor = system.actorOf(TcpActor.props(dataWriterClient))
      val channel = mock[Channel]
      connect(tcpActor, channel)

      val text: String = "text"
      val tcpMessage = TextTcpMessage(text)

      (channel.write(_: Any)).expects(text).once()
      val txNext = TestProbe()

      val session: Session = Session("Scenario", "User")
      tcpActor ! Send("request", tcpMessage, txNext.ref,session,None)

      val expectedSession = txNext.expectMsg(session)
      expectedSession.status shouldEqual OK
    }

    "do not send message to channel if not text message" in {
      val tcpActor = system.actorOf(TcpActor.props(dataWriterClient))
      val channel = mock[Channel]
      connect(tcpActor, channel)

      val text: String = "text"
      val tcpMessage = ByteTcpMessage(Array(0.toByte,1.toByte,2.toByte))

      (channel.write(_: Any)).expects(text).never()
      val txNext = TestProbe()

      val session: Session = Session("Scenario", "User")
      tcpActor ! Send("request", tcpMessage, txNext.ref,session,None)

      val expectedSession = txNext.expectMsg(session)
      expectedSession.status shouldEqual OK
    }
    "send message to channel and proceed next transaction with check" in {
      val tcpActor = system.actorOf(TcpActor.props(dataWriterClient))
      val channel = mock[Channel]
      val check = mock[Check[String]]
      val tcpCheck = TcpCheck(check, 200 millis, 0l)
      connect(tcpActor, channel)

      val text: String = "text"
      val tcpMessage = TextTcpMessage(text)

      (channel.write(_: Any)).expects(text).once()
      val txNext = TestProbe()

      val session: Session = Session("Scenario", "User")
      tcpActor ! Send("request", tcpMessage, txNext.ref,session,Some(tcpCheck))

      val expectedSession = txNext.expectMsgClass(300 millis, classOf[Session])
      expectedSession.status shouldEqual KO
    }

  }


}