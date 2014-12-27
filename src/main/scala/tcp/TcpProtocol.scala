package io.gatling.tcp

import io.gatling.core.config.Protocol

case class TcpProtocol(address: String,
                       port: Int) extends Protocol {
  override def warmUp(): Unit = {
    TcpEngine.start()
    super.warmUp()
  }
}
