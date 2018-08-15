package io.gatling.tcp

import java.nio.charset.Charset

case object TcpProtocolBuilderAddressStep {
  def address(address: String) = TcpProtocolBuilderPortStep(address)
}
case class TcpProtocolBuilderPortStep(address: String) {
  def port(port: Int) = TcpProtocolBuilder(address, port, None, 0)
}
// TODO Add TLS as an option here
case class TcpProtocolBuilder(address: String, port: Int, framer: Option[TcpFramer], tls: Int) {
  def lengthBased(offset: Int, length: Int, adjust: Int, strip: Int, tls: Int) = {
    TcpProtocolBuilder(address, port, Some(LengthBasedTcpFramer(offset, length, adjust, strip)), tls)
  }
  def lengthBased(length: Int, tls: Int): TcpProtocolBuilder = lengthBased(0, length, 0, length, tls)
  def delimiterBased(delimiters: String, strip: Boolean, charset: String = "UTF-8", tls: Int) = {
    TcpProtocolBuilder(address, port, Some(DelimiterBasedTcpFramer(delimiters.getBytes(Charset.forName(charset)), strip)), tls)
  }
  def protobufVarint = TcpProtocolBuilder(address, port, Some(ProtobufVarint32TcpFramer), 0)

  def build() = new TcpProtocol(address = address, port = port, framer = framer getOrElse(LengthBasedTcpFramer(0, 4, 0, 4)), tls = tls)
}
