import sbt.Keys._



name := "api-gateway"
ThisBuild / organization := "de.dnpm.dip"
ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version      := "1.0-SNAPSHOT"


scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-unchecked",
  "-language:postfixOps",
  "-Xfatal-warnings",
  "-feature",
  "-deprecation"
)


libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play"     % "5.1.0" % Test,  //TODO: version!

  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.11.4",

  "de.dnpm.dip"            %% "catalog-service-api"    % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "catalog-service-impl"   % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "service-base"           % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "rd-query-service-api"   % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "rd-query-service-impl"  % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "connector-base"         % "1.0-SNAPSHOT",
  "de.ekut.tbi"            %% "generators"             % "1.0-SNAPSHOT",


//  "de.dnpm.dip"            %% "..."  % "1.0-SNAPSHOT",
)


dependencyOverrides ++= Seq(
  "org.scala-lang.modules" %% "scala-xml"          % "2.2.0",
  "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.2",
)


lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .disablePlugins(PlayLogback)
  .settings()


