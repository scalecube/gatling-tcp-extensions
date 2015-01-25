package io.gatling.tcp

import java.nio.charset.Charset


case object TcpProtocolBuilderAddressStep {
  def address(address: String) = TcpProtocolBuilderPortStep(address)
}
case class TcpProtocolBuilderPortStep(address: String) {
  def port(port: Int) = TcpProtocolBuilderFramerStep(address, port)
}
case class TcpProtocolBuilderFramerStep(address: String, port: Int) {
  def lengthBased(offset : Int, length: Int, adjust: Int, strip : Int) = {
    TcpProtocolBuilder(address, port, LengthBasedTcpFramer(offset, length, adjust, strip))
  }
  def lengthBased(length: Int) = lengthBased(0, length, 0, length)
  def delimiterBased(delimiters : String, strip : Boolean, charset : String = "UTF-8") = {
    TcpProtocolBuilder(address,port, DelimiterBasedTcpFramer(delimiters.getBytes(Charset.forName(charset)),strip))
  }
  def protobufVarint = TcpProtocolBuilder(address, port, ProtobufVarint32TcpFramer)
}
case class TcpProtocolBuilder(address: String, port: Int, framer : TcpFramer) {
  def build() = new TcpProtocol(address = address, port = port, framer = framer)
}
