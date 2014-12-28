package io.gatling.tcp

import io.gatling.core.session.Expression
import io.gatling.tcp.check.TcpCheckDsl
import io.gatling.tcp.request.Tcp

object Predef extends TcpCheckDsl {

  val tcp = TcpProtocolBuilderAddressStep

  implicit def tcpBuilderToProtocol(builder: TcpProtocolBuilder): TcpProtocol = builder.build()

  def tcp(requestName: Expression[String]) = new Tcp(requestName)
}
