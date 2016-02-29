package io.gatling.tcp

import java.nio.charset.Charset

case object TcpProtocolBuilderAddressStep {
  def address(address: String) = TcpProtocolBuilderPortStep(address)
}
case class TcpProtocolBuilderPortStep(address: String) {
  def port(port: Int) = TcpProtocolBuilder(address, port, None)
}
case class TcpProtocolBuilder(address: String, port: Int, framer: Option[TcpFramer]) {
  def lengthBased(offset: Int, length: Int, adjust: Int, strip: Int) = {
    TcpProtocolBuilder(address, port, Some(LengthBasedTcpFramer(offset, length, adjust, strip)))
  }
  def lengthBased(length: Int): TcpProtocolBuilder = lengthBased(0, length, 0, length)
  def delimiterBased(delimiters: String, strip: Boolean, charset: String = "UTF-8") = {
    TcpProtocolBuilder(address, port, Some(DelimiterBasedTcpFramer(delimiters.getBytes(Charset.forName(charset)), strip)))
  }
  def protobufVarint = TcpProtocolBuilder(address, port, Some(ProtobufVarint32TcpFramer))

  def build() = new TcpProtocol(address = address, port = port, framer = framer getOrElse(LengthBasedTcpFramer(0, 4, 0, 4)))
}
