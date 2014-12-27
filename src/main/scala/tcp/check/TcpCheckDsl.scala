package io.gatling.tcp.check

import io.gatling.core.session._

import scala.concurrent.duration.FiniteDuration


trait TcpCheckDsl {

  val tcpCheck : Step2 = new Step2

  class Step2() {

    def within(timeout: FiniteDuration) = new Step3(timeout)
  }

  class Step3(timeout: FiniteDuration) {
    def regex(expression: Expression[String]) = TcpRegexCheckBuilder.regex(expression, TcpCheckBuilders.extender(timeout))
  }

}

