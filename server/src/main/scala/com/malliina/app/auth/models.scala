package com.malliina.app.auth

import com.malliina.http.FullUrl
import com.malliina.values.{Email, TokenValue}
import com.malliina.web.{ClientId, ClientSecret, Issuer}
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder}

import java.text.ParseException
import java.time.Instant
import scala.concurrent.duration.{Duration, DurationLong}

case class CognitoConf(id: ClientId, secret: ClientSecret, domain: FullUrl, issuer: Issuer)

case class CognitoUserInfo(sub: String, email_verified: String, email: Email, username: String):
  def emailVerified = email_verified == "true"

object CognitoUserInfo:
  implicit val json: Codec[CognitoUserInfo] = deriveCodec[CognitoUserInfo]
