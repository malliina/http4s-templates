package com.malliina.app

import com.malliina.app.Literals.url.{Expr, Quotes}
import com.malliina.app.Tokens.OpaqueToken
import com.malliina.http.FullUrl
import org.typelevel.literally.Literally

extension (inline ctx: StringContext)
  inline def jwt(inline args: Any*): OpaqueToken =
    ${ Tokens.literal('ctx, 'args) }

object Tokens:
  opaque type OpaqueToken = String

  object OpaqueToken:
    def apply(s: String): Either[String, OpaqueToken] = Option
      .when(s.count(_ == '.') == 2)(s)
      .toRight(s"Invalid token. Must contain exactly two dots.")

  extension (t: OpaqueToken)
    def length = t.length
    def trim = t.trim
    def duplicate = s"$t$t"

  object literal extends Literally[OpaqueToken]:
    def validate(s: String)(using Quotes): Either[String, Expr[OpaqueToken]] =
      OpaqueToken(s).map(t => '{ OpaqueToken(${ Expr(s) }).toOption.get })
