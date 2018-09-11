package io.gatling.tcp

import io.gatling.core.config.Protocol
import org.jboss.netty.handler.codec.frame.{LengthFieldBasedFrameDecoder, FrameDecoder}

case class TcpProtocol(address: String,
                       port: Int, framer : TcpFramer, tls: Option[TcpTls]) extends Protocol {
  override def warmUp(): Unit = {
    TcpEngine.start()
    super.warmUp()
  }
}
