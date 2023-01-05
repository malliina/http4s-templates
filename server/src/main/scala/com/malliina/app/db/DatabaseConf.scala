package com.malliina.app.db

import com.malliina.config.ConfigReadable
import com.malliina.config.ConfigReadable.ConfigOps

case class DatabaseConf(enabled: Boolean, url: String, user: String, pass: String)

object DatabaseConf:
  val MySQLDriver = "com.mysql.cj.jdbc.Driver"
  val DefaultDriver = MySQLDriver

  implicit val config: ConfigReadable[DatabaseConf] = ConfigReadable.config.emap { obj =>
    for
      enabled <- obj.read[Boolean]("enabled")
      url <- obj.read[String]("url")
      user <- obj.read[String]("user")
      pass <- obj.read[String]("pass")
    yield DatabaseConf(enabled, url, user, pass)
  }
