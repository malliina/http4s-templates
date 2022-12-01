scalaVersion := "2.12.17"

Seq(
  "com.eed3si9n" % "sbt-buildinfo" % "0.11.0",
  "io.spray" % "sbt-revolver" % "0.9.1",
  "org.scalameta" % "sbt-scalafmt" % "2.5.0",
  "com.github.sbt" % "sbt-native-packager" % "1.9.11",
  "com.eed3si9n" % "sbt-assembly" % "2.0.0"
) map addSbtPlugin
