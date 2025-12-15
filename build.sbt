import sbt.Keys._
import scala.util.Properties.envOrElse


name := "dnpm-dip-api-gateway"
ThisBuild / organization := "de.dnpm.dip"
ThisBuild / scalaVersion := "2.13.16"
ThisBuild / version      := envOrElse("VERSION","1.2.3-SNAPSHOT")

val ownerRepo  = envOrElse("REPOSITORY","dnpm-dip/api-gateway").split("/")
ThisBuild / githubOwner      := ownerRepo(0)
ThisBuild / githubRepository := ownerRepo(1)


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
  "de.ekut.tbi"            %% "generators"                  % "1.0.0",
  "de.dnpm.dip"            %% "admin-service-api"           % "1.1.1",
  "de.dnpm.dip"            %% "admin-service-impl"          % "1.1.1",
  "de.dnpm.dip"            %% "catalog-service-api"         % "1.1.0",
  "de.dnpm.dip"            %% "catalog-service-impl"        % "1.1.0",
  "de.dnpm.dip"            %% "service-base"                % "1.2.3",
  "de.dnpm.dip"            %% "mtb-validation-service-api"  % "1.1.6",
  "de.dnpm.dip"            %% "mtb-validation-service-impl" % "1.1.6",
  "de.dnpm.dip"            %% "mtb-query-service-api"       % "1.1.2",
  "de.dnpm.dip"            %% "mtb-query-service-impl"      % "1.1.2",
  "de.dnpm.dip"            %% "rd-validation-service-api"   % "1.1.7",
  "de.dnpm.dip"            %% "rd-validation-service-impl"  % "1.1.7",
  "de.dnpm.dip"            %% "rd-query-service-api"        % "1.1.3",
  "de.dnpm.dip"            %% "rd-query-service-impl"       % "1.1.3",
  "de.dnpm.dip"            %% "connector-base"              % "1.1.1",
  "de.dnpm.dip"            %% "hp-ontology"                 % "1.1.2",
  "de.dnpm.dip"            %% "alpha-id-se"                 % "1.1.2",
  "de.dnpm.dip"            %% "orphanet-ordo"               % "1.1.2",
  "de.dnpm.dip"            %% "hgnc-gene-set-impl"          % "1.1.1",
  "de.dnpm.dip"            %% "icd10gm-impl"                % "1.1.2",
  "de.dnpm.dip"            %% "icdo3-impl"                  % "1.1.2",
  "de.dnpm.dip"            %% "icd-claml-packaged"          % "1.1.2",
  "de.dnpm.dip"            %% "atc-impl"                    % "1.1.0",
  "de.dnpm.dip"            %% "atc-catalogs-packaged"       % "1.1.0",
  "de.dnpm.dip"            %% "auth-api"                    % "1.1.0",
  "de.dnpm.dip"            %% "standalone-authup-client"    % "1.1.0",
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
)

resolvers ++= Seq(
  "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
  Resolver.githubPackages("dnpm-dip"),
  Resolver.githubPackages("KohlbacherLab"),
  Resolver.sonatypeCentralSnapshots
)
