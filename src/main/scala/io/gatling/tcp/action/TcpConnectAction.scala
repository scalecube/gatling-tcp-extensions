package io.gatling.tcp.action

import akka.actor.ActorDSL._
import akka.actor.ActorRef
import io.gatling.core.action.{ Failable, Interruptable }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.TimeHelper._
import io.gatling.core.validation.Validation
import io.gatling.tcp._

class TcpConnectAction(requestName: Expression[String], val next: ActorRef, protocol: TcpProtocol) extends Interruptable with Failable {
  /**
   * Core method executed when the Action received a Session message
   *
   * @param session the session of the virtual user
   * @return Nothing
   */

  override def executeOrFail(session: Session): Validation[_] = {
      def connect(tx: TcpTx): Unit = {
        //  logger.info(s"Opening websocket '$wsName': Scenario '${session.scenarioName}', UserId #${session.userId}")

        val tcpActor = actor(context, actorName("tcpActor"))(new TcpActor())
        TcpEngine.instance.startTcpTransaction(tx, tcpActor)

      }

    for {
      requestName <- requestName(session)
    } yield connect(TcpTx(session, next, requestName = requestName, protocol = protocol, message = TextTcpMessage(""), start = nowMillis))
  }
}
