inThisBuild(
  Seq(
    organization := "com.malliina",
    version := "0.0.1",
    scalaVersion := "3.1.1"
  )
)

val app = project
  .in(file("."))
  .enablePlugins(RevolverPlugin)
  .settings(
    libraryDependencies ++=
      Seq("ember-server", "dsl", "circe").map { m => "org.http4s" %% s"http4s-$m" % "0.23.10" } ++
      Seq("core", "generic").map { m => "io.circe" %% s"circe-$m" % "0.14.1" } ++
      Seq("classic", "core").map { m => "ch.qos.logback" % s"logback-$m" % "1.2.10" } ++
      Seq(
        "org.slf4j" % "slf4j-api" % "1.7.36",
        "com.lihaoyi" %% "scalatags" % "0.11.1",
        "org.scalameta" %% "munit" % "0.7.29" % Test,
        "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test,
        "com.malliina" %% "okclient-io" % "3.1.0" % Test
      ),
    testFrameworks += new TestFramework("munit.Framework")
  )

val infra = project.in(file("infra")).settings(
  libraryDependencies ++= Seq(
    "software.amazon.awscdk" % "aws-cdk-lib" % "2.12.0"
  )
)

Global / onChangedBuildSource := ReloadOnSourceChanges
