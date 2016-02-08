import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.tcp.Predef._

import scala.concurrent.duration._

class TcpCompile extends Simulation {

  val tcpConfig = tcp.address("127.0.0.1").port(4800).lengthBased(4)
  val scn = scenario("Tcp")
    .exec(tcp("Connect").connect())
    .pause(1)
    .repeat(2, "i") {
       exec(tcp("Say Hello Tcp")
      .sendText( """{"qualifier":"someQualifier","data":{"properties":null}}""")
       .check(tcpCheck.within(5 seconds).regex( """"context":"(.+?)"""").saveAs("context"))
       ).pause(1)
  }
    .exec(tcp("disconnect").disconnect())

  setUp(scn.inject(atOnceUsers(5))).protocols(tcpConfig)

}
