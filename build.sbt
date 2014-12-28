val scala_version = "2.11.4"
val gatling = "io.gatling" % "gatling-core" % "2.1.2"
private val netty = "io.netty" % "netty" % "3.9.5.Final"
private val akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.3.7"
private val scalalogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
private def scalaLibrary = "org.scala-lang" % "scala-library" % scala_version


lazy val root = (project in file(".")).
  settings(
    organization := "com.github",
    version := "0.1.0",
    scalaVersion := scala_version,
    name := "gatling-tcp-extension",
    libraryDependencies += gatling,
    libraryDependencies += netty,
    libraryDependencies += akkaActor,
    libraryDependencies += scalalogging
  )