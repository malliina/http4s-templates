package com.malliina.app.auth

import cats.MonadError
import cats.effect.IO
import cats.implicits.{toFlatMapOps, toFunctorOps}
import com.malliina.http.io.{HttpClientF, HttpClientIO}
import com.malliina.http.{FullUrl, OkHttpResponse}
import com.malliina.values.{AccessToken, Email}
import com.malliina.web.{CognitoIdValidator, CognitoTokens, CognitoTokensJson}

import java.nio.charset.StandardCharsets
import java.util.Base64

class Cognito[F[_]](conf: CognitoConf, http: HttpClientF[F])(implicit F: MonadError[F, Throwable]):
  val domain = conf.domain
  val tokenUrl = domain / "oauth2" / "token"
  val userInfoUrl = domain / "oauth2" / "userInfo"
  private val authorizeBaseUrl = domain / "oauth2" / "authorize"
  private val logoutBaseUrl = domain / "logout"
  val loginCallback = "http://localhost:9000/auth/callback"
  val logoutCallback = "http://localhost:9000/auth/logout/callback"
  private val authorizeParams = Map(
    "identity_provider" -> "Google",
    "redirect_uri" -> loginCallback,
    "response_type" -> "code",
    "client_id" -> conf.id.value,
    "scope" -> "openid email"
  )
  val authorizeUrl = authorizeBaseUrl.query(authorizeParams)
  val changeUserUrl = logoutBaseUrl.query(authorizeParams)
  val logoutUrl = logoutBaseUrl.query(
    Map(
      "client_id" -> conf.id.value,
      "logout_uri" -> logoutCallback
    )
  )

  def email(code: String): F[Email] =
    for
      tokens <- exchange(code)
      info <- userInfo(tokens.accessToken)
    yield info.email

  def exchange(code: String): F[CognitoTokens] =
    http
      .postFormAs[CognitoTokensJson](
        tokenUrl,
        Map(
          "grant_type" -> "authorization_code",
          "client_id" -> conf.id.value,
          "code" -> code,
          "redirect_uri" -> "http://localhost:9000/auth/callback"
        ),
        Map(
          "Authorization" -> authorizationValue(conf.id.value, conf.secret.value)
        )
      )
      .map(_.canonical)

  def userInfo(accessToken: AccessToken): F[CognitoUserInfo] =
    http.getAs[CognitoUserInfo](userInfoUrl, Map("Authorization" -> s"Bearer $accessToken"))

  def authorizationValue(username: String, password: String): String =
    val encoded = s"$username:$password".getBytes(StandardCharsets.UTF_8)
    val encodedString = Base64.getEncoder.encodeToString(encoded)
    s"Basic $encodedString"
