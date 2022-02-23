scalaVersion := "2.12.15"

Seq(
  "com.eed3si9n" % "sbt-buildinfo" % "0.9.0",
  "io.spray" % "sbt-revolver" % "0.9.1",
  "org.scalameta" % "sbt-scalafmt" % "2.4.3",
  "com.github.sbt" % "sbt-native-packager" % "1.9.7",
  "com.eed3si9n" % "sbt-assembly" % "1.2.0"
) map addSbtPlugin
