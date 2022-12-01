import com.typesafe.sbt.packager.MappingsHelper.{contentOf, directory}

import scala.sys.process.Process
import scala.util.Try

inThisBuild(
  Seq(
    organization := "com.malliina",
    version := "0.0.1",
    scalaVersion := "3.2.1"
  )
)

val assetsDir = settingKey[File]("Path to public assets")

val server = project
  .in(file("server"))
  .enablePlugins(RevolverPlugin, BuildInfoPlugin, JavaServerAppPackaging)
  .settings(
    libraryDependencies ++=
      Seq("ember-server", "dsl", "circe").map { m => "org.http4s" %% s"http4s-$m" % "0.23.16" } ++
        Seq("core", "generic").map { m => "io.circe" %% s"circe-$m" % "0.14.3" } ++
        Seq("classic", "core").map { m => "ch.qos.logback" % s"logback-$m" % "1.2.11" } ++
        Seq(
          "org.slf4j" % "slf4j-api" % "1.7.36",
          "com.lihaoyi" %% "scalatags" % "0.12.0",
          "com.malliina" %% "okclient-io" % "3.3.0",
          "org.scalameta" %% "munit" % "0.7.29" % Test,
          "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test
        ),
    testFrameworks += new TestFramework("munit.Framework"),
    assetsDir := baseDirectory.value / "assets" / "public",
    Universal / javaOptions ++= Seq("-J-Xmx256m"),
    Universal / mappings ++=
      contentOf(baseDirectory.value / "src" / "universal") ++ directory(assetsDir.value),
    buildInfoPackage := "com.malliina.app.build",
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      "assetsDir" -> assetsDir.value.relativeTo(baseDirectory.value).get.toString,
      "assetsPath" -> "public",
      "gitHash" -> gitHash
    ),
    assembly / assemblyJarName := "app.jar",
    Compile / unmanagedResourceDirectories += baseDirectory.value / "assets"
  )

val infra = project
  .in(file("infra"))
  .disablePlugins(RevolverPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "software.amazon.awscdk" % "aws-cdk-lib" % "2.53.0"
    )
  )

val root = project
  .in(file("."))
  .disablePlugins(RevolverPlugin)
  .aggregate(server, infra)

def gitHash = Try(Process("git rev-parse --short HEAD").lineStream.head).toOption
  .orElse(sys.env.get("CODEBUILD_RESOLVED_SOURCE_VERSION").map(_.take(7)))
  .orElse(sys.env.get("CODEBUILD_SOURCE_VERSION").map(_.take(7)))
  .getOrElse("latest")

Global / onChangedBuildSource := ReloadOnSourceChanges
