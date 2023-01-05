package com.malliina.app

import com.malliina.app.build.BuildInfo
import com.malliina.app.db.DatabaseConf
import com.malliina.values.ErrorMessage
import com.typesafe.config.{Config, ConfigFactory}
import com.malliina.config.ConfigReadable.ConfigOps

import java.nio.file.{Path, Paths}

case class RefConf(db: DatabaseConf)

object RefConf:
  val appDir = Paths.get(sys.props("user.home")).resolve(".refapp")
  val localConfFile = appDir.resolve("refapp.conf")
  val localConfig = ConfigFactory.parseFile(localConfFile.toFile).withFallback(ConfigFactory.load())

  val isProd = false
  private def refConf =
    val conf =
      if isProd then ConfigFactory.load("application-prod.conf").resolve()
      else ConfigFactory.load(localConfig).resolve()
    conf.getConfig("ref")

  def unsafe(c: Config = refConf): RefConf =
    parse(c).fold(err => throw ConfigException(err), identity)

  def parse(c: Config): Either[ErrorMessage, RefConf] =
    for
//      mode <- c.read[AppMode]("mode")
      db <- c.read[DatabaseConf]("db")
    yield RefConf(db)

class ConfigException(error: ErrorMessage) extends Exception(error.message)
