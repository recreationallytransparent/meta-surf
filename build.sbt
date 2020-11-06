name := "MetaSurf"
organization := "com.pickard"
version := "0.0.1"
scalaVersion := "2.12.8"

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

resolvers += Resolver.jcenterRepo

val elastic4sVersion = "7.1.0"
libraryDependencies ++= Seq(
  "net.ruippeixotog" %% "scala-scraper" % "1.2.0",
  "com.typesafe.akka" %% "akka-http"   % "10.1.11",
  "com.typesafe.akka" %% "akka-stream" % "2.5.26",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.11",
  "org.json4s" %% "json4s-native" % "latest.release",
  "org.json4s" %% "json4s-jackson" % "latest.release",
  "com.github.nikita-volkov" % "sext" % "0.2.4",
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.8.0",
  "org.slf4j" % "slf4j-simple" % "1.7.30",
  "org.rogach" %% "scallop" % "3.4.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
)
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.8"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % "test"