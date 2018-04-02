import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.4",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "akka-persistence-cassandra-poc",
    libraryDependencies ++= Seq(
      akkaHttp,
      akkaPersistenceTyped,
      akkaPersistenceCassandra,
      scalaTest % Test
    )
  )
