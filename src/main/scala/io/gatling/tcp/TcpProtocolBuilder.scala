package io.gatling.tcp

import java.nio.charset.Charset

case object TcpProtocolBuilderAddressStep {
  def address(address: String) = TcpProtocolBuilderPortStep(address)
}
case class TcpProtocolBuilderPortStep(address: String) {
  def port(port: Int) = TcpProtocolBuilder(address, port, None, None)
}

case class TcpTls(ver: String, trustStoreResource: String, password: String) {

}



// TODO Add TLS as an option here
case class TcpProtocolBuilder(address: String, port: Int, framer: Option[TcpFramer], tlsLayer: Option[TcpTls]) {
  def lengthBased(offset: Int, length: Int, adjust: Int, strip: Int) = {
    TcpProtocolBuilder(address, port, Some(LengthBasedTcpFramer(offset, length, adjust, strip)), tlsLayer)
  }
  def lengthBased(length: Int): TcpProtocolBuilder = lengthBased(0, length, 0, length)
  def delimiterBased(delimiters: String, strip: Boolean, charset: String = "UTF-8") = {
    TcpProtocolBuilder(address, port, Some(DelimiterBasedTcpFramer(delimiters.getBytes(Charset.forName(charset)), strip)), tlsLayer)
  }
  def protobufVarint = TcpProtocolBuilder(address, port, Some(ProtobufVarint32TcpFramer), tlsLayer)

  def tls(ver: String, trustStoreResource: String, password: String) = TcpProtocolBuilder(address, port, framer, Some(TcpTls(ver, trustStoreResource, password)))

  def build() = new TcpProtocol(address = address, port = port, framer = framer getOrElse(LengthBasedTcpFramer(0, 4, 0, 4)), tls = tlsLayer)
}
