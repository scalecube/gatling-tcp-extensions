import io.gatling.sbt.GatlingPlugin

val scala_version = "2.11.4"
def gatling = "io.gatling" % "gatling-core" % "2.1.5"
def netty = "io.netty" % "netty" % "3.10.1.Final"
def akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.3.7"
def scalalogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
def scalaLibrary = "org.scala-lang" % "scala-library" % scala_version
def highcharts = "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.1.5" % "test"
def gatlingtestframework = "io.gatling" % "gatling-test-framework" % "2.1.5" % "test"


lazy val root = (project in file(".")).
  settings(
    organization := "com.github",
    version := "0.1.0",
    scalaVersion := scala_version,
    name := "gatling-tcp-extension",
    libraryDependencies += gatling,
    libraryDependencies += netty,
    libraryDependencies += akkaActor,
    libraryDependencies += scalalogging,
    libraryDependencies += highcharts,
    libraryDependencies += gatlingtestframework
  )

enablePlugins(GatlingPlugin)
