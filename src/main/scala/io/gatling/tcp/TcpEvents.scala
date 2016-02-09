package io.gatling.tcp

import akka.actor.ActorRef
import io.gatling.core.check.Check
import io.gatling.core.session.Session
import io.gatling.tcp.check.TcpCheck
import org.jboss.netty.channel.Channel

sealed trait TcpEvents

case class OnConnect(tx: TcpTx, channel: Channel, time: Long) extends TcpEvents
case class OnConnectFailed(tx: TcpTx, time: Long) extends TcpEvents
case class OnDisconnect(time: Long) extends TcpEvents
case class OnTextMessage(message: String, time: Long) extends TcpEvents
case class CheckTimeout(check: TcpCheck) extends TcpEvents

sealed trait TcpUserActions
case object Connect extends TcpUserActions
case class Send(requestName: String, message: TcpMessage, next: ActorRef, session: Session, check: Option[TcpCheck]) extends TcpUserActions
case class Disconnect(requestName: String, next: ActorRef, session: Session) extends TcpUserActions
