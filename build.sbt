name := """scalaIO"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "2.0.0-M3",
  "com.typesafe.play" %% "play-json" % "2.6.10"
)

val sttpVersion = "1.5.16"
libraryDependencies ++= Seq(
  "com.softwaremill.sttp" %% "core",
  "com.softwaremill.sttp" %% "async-http-client-backend-monix",
  "com.softwaremill.sttp" %% "async-http-client-backend-fs2",
  "com.softwaremill.sttp" %% "async-http-client-backend-zio-streams"
).map(_ % sttpVersion)

val circeVersion = "0.10.0"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-fs2"
).map(_ % circeVersion)
