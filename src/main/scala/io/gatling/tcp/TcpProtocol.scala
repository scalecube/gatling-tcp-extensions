package io.gatling.tcp

import io.gatling.core.config.Protocol
import org.jboss.netty.handler.codec.frame.{LengthFieldBasedFrameDecoder, FrameDecoder}

case class TcpProtocol(address: String,
                       port: Int, framer : TcpFramer) extends Protocol {
  override def warmUp(): Unit = {
    TcpEngine.start()
    super.warmUp()
  }
}
