package io.gatling.tcp.action

import akka.actor.ActorDSL._
import akka.actor.ActorRef
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.config.Protocols
import io.gatling.core.session.Expression
import io.gatling.tcp.{ TcpCheckBuilder, TcpProtocol, TcpMessage }

class TcpConnectActionBuilder(requestName: Expression[String]) extends ActionBuilder {
  /**
   * @param next the Action that will be chained with the Action build by this builder
   * @param protocols the protocols configurations
   * @return the resulting Action actor
   */
  override def build(next: ActorRef, protocols: Protocols): ActorRef = actor(actorName("tcpConnect"))(new TcpConnectAction(requestName, next,
    protocols.getProtocol[TcpProtocol].getOrElse(throw new UnsupportedOperationException("Tcp Protocol wasn't registered"))))
}

class TcpSendActionBuilder(requestName: Expression[String], message: Expression[TcpMessage], checkBuilder: Option[TcpCheckBuilder] = None) extends ActionBuilder {
  /**
   * @param next the Action that will be chained with the Action build by this builder
   * @param protocols the protocols configurations
   * @return the resulting Action actor
   */
  override def build(next: ActorRef, protocols: Protocols): ActorRef = actor(actorName("tcpSend"))(new TcpSendAction(requestName, next, message, checkBuilder))

  def check(checkBuilder: TcpCheckBuilder) = new TcpSendActionBuilder(requestName, message, Some(checkBuilder))
}

class TcpDisconnectActionBuilder(requestName: Expression[String]) extends ActionBuilder {
  /**
   * @param next the Action that will be chained with the Action build by this builder
   * @param protocols the protocols configurations
   * @return the resulting Action actor
   */
  override def build(next: ActorRef, protocols: Protocols): ActorRef = actor(actorName("tcpDisconnect"))(new TcpCloseAction(requestName, next))
}