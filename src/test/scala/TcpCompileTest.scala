import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.tcp.Predef._

import scala.concurrent.duration._

class TcpCompileTest extends Simulation {

  val tcpConfig = tcp.address("127.0.0.1").port(4800)
  val scn = scenario("Tcp")
    .exec(tcp("Connect").connect())
    .pause(1)
    .repeat(2, "i") {
       exec(tcp("Say Hello Tcp")
      .sendText( """{"qualifier":"pt.openapi.context/createContextRequest","data":{"properties":null}}""")
       .check(tcpCheck.within(5 seconds).regex( """"contextId":"(.+?)"""").saveAs("contextId"))
       ).pause(1)
  }
    .exec(tcp("disconnect").disconnect())

  setUp(scn.inject(atOnceUsers(5))).protocols(tcpConfig)

}
