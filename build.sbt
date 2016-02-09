import io.gatling.sbt.GatlingPlugin

val scala_version = "2.11.4"
val akka_version ="2.3.7"

def gatling = "io.gatling" % "gatling-core" % "2.1.5"
def netty = "io.netty" % "netty" % "3.10.1.Final"
def akkaActor = "com.typesafe.akka" %% "akka-actor" % akka_version
def scalalogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
def scalaLibrary = "org.scala-lang" % "scala-library" % scala_version
def highcharts = "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.1.5" % "test"
def gatlingtestframework = "io.gatling" % "gatling-test-framework" % "2.1.5" % "test"
def akkaTest = "com.typesafe.akka" %% "akka-testkit" % akka_version % "test"


lazy val root = (project in file(".")).
  settings(
    organization := "io.scalecube",
    version := (version in ThisBuild).value,
    scalaVersion := scala_version,
    name := "gatling-tcp-extension",
    libraryDependencies += gatling,
    libraryDependencies += netty,
    libraryDependencies += akkaActor,
    libraryDependencies += scalalogging,
    libraryDependencies += highcharts,
    libraryDependencies += gatlingtestframework,
    libraryDependencies += akkaTest,
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test",
    libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % "test"
  )

enablePlugins(GatlingPlugin)

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := <url>https://github.com/scalecube/gatling-tcp-extensions</url>
  <licenses>
    <license>
      <name>The MIT License (MIT)</name>
      <url>https://opensource.org/licenses/MIT</url>
    </license>
  </licenses>
  <scm>
    <url>https://github.com/scalecube/gatling-tcp-extensions.git</url>
    <connection>scm:git:https://github.com/scalecube/gatling-tcp-extensions.git</connection>
  </scm>
  <developers>
    <developer>
      <id>myroslavlisniak</id>
      <name>Myroslav Lisniak</name>
    </developer>
  </developers>