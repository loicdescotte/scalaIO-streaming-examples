name := """scalaIO"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "2.0.0-M3",
  "com.typesafe.play" %% "play-json" % "2.6.10",
  "com.softwaremill.sttp" %% "core" % "1.3.2",
  "com.softwaremill.sttp" %% "async-http-client-backend-monix" % "1.3.5",
  "com.softwaremill.sttp" %% "async-http-client-backend-fs2" % "1.3.5",
  "com.softwaremill.sttp" %% "async-http-client-backend-fs2" % "1.3.5"
)

val circeVersion = "0.10.0"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-fs2"
).map(_ % circeVersion)