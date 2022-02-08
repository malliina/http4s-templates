package com.malliina.app

import cats.Monad
import cats.effect.Async
import com.malliina.app.ErrorHandler.log
import io.circe.syntax.EncoderOps
import org.http4s.headers.{Connection, `Content-Length`}
import org.http4s.{Headers, Request, Response, Status}
import org.typelevel.ci.CIStringSyntax

import scala.util.control.NonFatal

object ErrorHandler:
  private val log = AppLogger(getClass)

class ErrorHandler[F[_]: Async] extends BasicService[F]:
  def apply: Request[F] => PartialFunction[Throwable, F[Response[F]]] =
    req => { case NonFatal(t) =>
      log.error(s"Server error: ${req.method} ${req.pathInfo}. Exception $t", t)
      serverError(req)
    }
