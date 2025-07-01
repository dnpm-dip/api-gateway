import sbt.Keys._



name := "dnpm-dip-api-gateway"
ThisBuild / organization := "de.dnpm.dip"
ThisBuild / scalaVersion := "2.13.16"
ThisBuild / version      := "1.0-SNAPSHOT"



lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion),
    buildInfoPackage := "de.dnpm.dip.rest.api"
  )


libraryDependencies ++= Seq(
  caffeine,
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play"          % "7.0.1" % Test,  //TODO: version!
  "de.ekut.tbi"            %% "generators"                  % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "admin-service-api"           % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "admin-service-impl"          % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "catalog-service-api"         % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "catalog-service-impl"        % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "service-base"                % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "mtb-validation-service-api"  % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "mtb-validation-service-impl" % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "mtb-query-service-api"       % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "mtb-query-service-impl"      % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "rd-validation-service-api"   % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "rd-validation-service-impl"  % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "rd-query-service-api"        % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "rd-query-service-impl"       % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "connector-base"              % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "hp-ontology"                 % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "alpha-id-se"                 % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "orphanet-ordo"               % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "hgnc-gene-set-impl"          % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "icd10gm-impl"                % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "icdo3-impl"                  % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "icd-claml-packaged"          % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "atc-impl"                    % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "atc-catalogs-packaged"       % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "auth-api"                    % "1.0-SNAPSHOT",
//  "de.dnpm.dip"            %% "fake-auth-service"           % "1.0-SNAPSHOT",
  "de.dnpm.dip"            %% "standalone-authup-client"    % "1.0-SNAPSHOT",
  
)


// Compiler options from: https://alexn.org/blog/2020/05/26/scala-fatal-warnings/
scalacOptions ++= Seq(
  // Feature options
  "-encoding", "utf-8",
  "-explaintypes",
  "-feature",
  "-language:existentials",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-Ymacro-annotations",

  // Warnings as errors!
  "-Xfatal-warnings",

  // Linting options
  "-unchecked",
  "-Xcheckinit",
  "-Xlint:adapted-args",
  "-Xlint:constant",
  "-Xlint:delayedinit-select",
  "-Xlint:deprecation",
  "-Xlint:doc-detached",
  "-Xlint:inaccessible",
  "-Xlint:infer-any",
  "-Xlint:missing-interpolator",
  "-Xlint:nullary-unit",
  "-Xlint:option-implicit",
  "-Xlint:package-object-classes",
  "-Xlint:poly-implicit-overload",
  "-Xlint:private-shadow",
  "-Xlint:stars-align",
  "-Xlint:type-parameter-shadow",
  "-Wdead-code",
  "-Wextra-implicit",
  "-Wnumeric-widen",
  "-Wunused:imports",
  "-Wunused:locals",
  "-Wunused:patvars",
  "-Wunused:privates",
  "-Wunused:implicits",
  "-Wvalue-discard",

  // To avoid many false positive errors about "unused import" etc from conf/routes
  "-Wconf:src=target/.*:s"

  // Deactivated to avoid many false positive errors from 'evidence' parameters in context bounds
  // "-Wunused:params",
)


