package io.gatling.tcp.action

import akka.actor.ActorRef
import akka.io.Tcp.Close
import io.gatling.core.action.{ Failable, Interruptable }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.Validation
import io.gatling.tcp.{ Disconnect, Send }

class TcpCloseAction(val requestName: Expression[String], val next: ActorRef) extends Interruptable with Failable {
  override def executeOrFail(session: Session): Validation[_] = {

    for {
      resolvedRequestName <- requestName(session)
      tcpActor <- session("tcpActor").validate[ActorRef].mapError(m => s"Couldn't fetch open websocket: $m")
    } yield tcpActor ! Disconnect(resolvedRequestName, next, session)
  }

}
