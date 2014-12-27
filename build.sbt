val scala_version = "2.10.4"
val gatling = "io.gatling" % "gatling-core" % "2.0.3"
private val netty = "io.netty" % "netty" % "3.9.5.Final"
private val akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.3.7"
private val scalalogging = "com.typesafe" %% "scalalogging-slf4j" % "1.1.0"
private def scalaLibrary = "org.scala-lang" % "scala-library" % scala_version


lazy val root = (project in file(".")).
  settings(
    organization := "com.ogp",
    version := "0.1.0",
    scalaVersion := scala_version,
    name := "gatling-tcp-extension",
    libraryDependencies += gatling,
    libraryDependencies += netty,
    libraryDependencies += akkaActor,
    libraryDependencies += scalalogging
  )