package com.malliina.app

import cats.data.NonEmptyList
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class SingleError(message: String, key: String)

object SingleError:
  implicit val json: Codec[SingleError] = deriveCodec[SingleError]

  def apply(message: String): SingleError = apply(message, "generic")

case class Errors(errors: NonEmptyList[SingleError])

object Errors:
  implicit val json: Codec[Errors] = deriveCodec[Errors]

  def apply(message: String): Errors = Errors(NonEmptyList.of(SingleError(message)))
