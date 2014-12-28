package io.gatling.tcp

import io.gatling.core.session.Expression

sealed trait TcpMessage

case class TextTcpMessage(message: String) extends TcpMessage
case class ByteTcpMessage(message: Array[Byte]) extends TcpMessage
