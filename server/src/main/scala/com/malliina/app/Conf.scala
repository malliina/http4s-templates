package com.malliina.app

import com.malliina.app.auth.CognitoConf
import com.malliina.http.FullUrl
import com.malliina.web.{ClientId, ClientSecret, Issuer}
import com.typesafe.config.{Config, ConfigFactory}
import com.malliina.config.ConfigReadable
import com.malliina.values.ErrorMessage

import java.nio.file.Paths

object Conf:
  private val appDir = Paths.get(sys.props("user.home")).resolve(".refapp")
  private val localConfFile = appDir.resolve("refapp.conf")
  private val localConfig =
    ConfigFactory.parseFile(localConfFile.toFile).withFallback(ConfigFactory.load())
  val conf = localConfig.resolve()

  implicit class ConfigOps(c: Config) extends AnyVal:
    def read[T](key: String)(implicit r: ConfigReadable[T]): Either[ErrorMessage, T] =
      r.read(key, c)
    def unsafe[T: ConfigReadable](key: String): T =
      c.read[T](key).fold(err => throw new IllegalArgumentException(err.message), identity)

  def unsafe =
    val cognito = conf.getConfig("cognito")
    CognitoConf(
      cognito.unsafe[ClientId]("id"),
      cognito.unsafe[ClientSecret]("secret"),
      cognito.unsafe[FullUrl]("domain"),
      cognito.unsafe[Issuer]("issuer")
    )
