package com.malliina.app

import com.malliina.http.FullUrl
import org.typelevel.literally.Literally

/** This enables syntax like `url"https://www.google.com"`, returning a `FullUrl`, and failing to
  * compile if the URL is invalid.
  */
extension (inline ctx: StringContext)
  inline def url(inline args: Any*): FullUrl =
    ${ Literals.url('ctx, 'args) }

object Literals:
  object url extends Literally[FullUrl]:
    def validate(s: String)(using Quotes): Either[String, Expr[FullUrl]] =
      FullUrl
        .build(s)
        .fold(
          err => Left(err.message),
          ok => Right('{ FullUrl.build(${ Expr(s) }).toOption.get })
        )
