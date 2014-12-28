package io.gatling

import io.gatling.core.check.CheckBuilder
import io.gatling.tcp.check.TcpCheck

package object tcp {
  type TcpCheckBuilder = CheckBuilder[TcpCheck, String, _, String]
}
