import sbt.Keys._



name := "dnpm-dip-api-gateway"
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



//  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.11.4",

libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play"        % "7.0.1" % Test,  //TODO: version!
  "de.ekut.tbi"            %% "generators"                % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "catalog-service-api"       % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "catalog-service-impl"      % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "service-base"              % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "mtb-query-service-api"     % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "mtb-query-service-impl"    % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "rd-query-service-api"      % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "rd-query-service-impl"     % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "connector-base"            % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "hp-ontology"               % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "omim-catalog"              % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "orphanet-ordo"             % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "hgnc-gene-set-impl"        % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "icd10gm-impl"              % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "icdo3-impl"                % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "icd-claml-packaged"        % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "atc-impl"                  % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "atc-catalogs-packaged"     % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "auth-api"                  % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "fake-auth-service"         % "1.0-SNAPSHOT",
//  "de.dnpm.dip"            %% "standalone-authup-client"  % "1.0-SNAPSHOT",

)


lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings()


