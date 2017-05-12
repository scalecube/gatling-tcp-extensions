package io.gatling.tcp

import io.gatling.core.protocol.Protocol

case class TcpProtocol(address: String,
                       port: Int, framer : TcpFramer) extends Protocol {
  override def warmUp(): Unit = {
    TcpEngine.start()
    super.warmUp()
  }
}
