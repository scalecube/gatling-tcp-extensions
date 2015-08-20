val scala_version = "2.11.4"
val gatling = "io.gatling" % "gatling-core" % "2.1.7"


lazy val root = (project in file(".")).
  settings(
    organization := "com.github",
    version := "0.1.0",
    scalaVersion := scala_version,
    name := "gatling-tcp-extension",
    libraryDependencies += "io.gatling" % "gatling-core" % "2.1.7",
    libraryDependencies +="io.netty" % "netty" % "3.10.4.Final",
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.7",
    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
  )