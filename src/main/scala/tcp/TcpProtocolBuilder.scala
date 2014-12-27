package io.gatling.tcp

case object TcpProtocolBuilderAddressStep {
  def address(address: String) = TcpProtocolBuilderPortStep(address)
}
case class TcpProtocolBuilderPortStep(address: String) {
  def port(port: Int) = TcpProtocolBuilder(address, port)
}

case class TcpProtocolBuilder(address: String, port: Int) {
  def build() = new TcpProtocol(address = address, port = port)
}
