import sbt._

object Dependencies {

  object Versions {
    val akkaV = "2.5.11"
    val akkaHttpV = "10.1.0"
    val akkaHttpCirce = "1.20.0"
    val akkaPersistenceCassandra = "0.83"
  }
  
  lazy val akkaClusterSharding      = "com.typesafe.akka"        %% "akka-cluster-sharding"        % Versions.akkaV
  lazy val akkaClusterTools         = "com.typesafe.akka"        %% "akka-cluster-tools"           % Versions.akkaV
  lazy val akkaDistributedData      = "com.typesafe.akka"        %% "akka-distributed-data"        % Versions.akkaV
  lazy val akkaHttp                 = "com.typesafe.akka"        %% "akka-http"                    % Versions.akkaHttpV
  lazy val akkaHttpTestkit          = "com.typesafe.akka"        %% "akka-http-testkit"            % Versions.akkaHttpV
  lazy val akkaHttpCirce            = "de.heikoseeberger"        %% "akka-http-circe"              % Versions.akkaHttpCirce
  lazy val akkaPersistence          = "com.typesafe.akka"        %% "akka-persistence"             % Versions.akkaV
  lazy val akkaPersistenceTyped     = "com.typesafe.akka"        %% "akka-persistence-typed"       % Versions.akkaV
  lazy val akkaPersistenceCassandra = "com.typesafe.akka"        %% "akka-persistence-cassandra"   % Versions.akkaPersistenceCassandra
  lazy val akkaPersistenceQuery     = "com.typesafe.akka"        %% "akka-persistence-query"       % Versions.akkaV
  lazy val akkaStream               = "com.typesafe.akka"        %% "akka-stream"                  % Versions.akkaV
  lazy val akkaTestkit              = "com.typesafe.akka"        %% "akka-testkit"                 % Versions.akkaV
  lazy val scalaTest                = "org.scalatest"            %% "scalatest"                    % "3.0.5"
}
