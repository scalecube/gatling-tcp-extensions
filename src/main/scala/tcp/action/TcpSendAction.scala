package io.gatling.tcp.action

import akka.actor.ActorRef
import io.gatling.core.action.{ Interruptable, Chainable, Failable }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.Validation
import io.gatling.tcp.{ Send, TcpMessage }

class TcpSendAction(val requestName: Expression[String], val next: ActorRef, message: Expression[TcpMessage]) extends Interruptable with Failable {
  override def executeOrFail(session: Session): Validation[_] = {
    for {
      resolvedRequestName <- requestName(session)
      tcpActor <- session("tcpActor").validate[ActorRef].mapError(m => s"Couldn't fetch tcp channel: $m")
      resolvedMessage <- message(session)
    } yield tcpActor ! Send(resolvedRequestName, resolvedMessage, next, session)
  }

}
