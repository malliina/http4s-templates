package com.malliina.app

import cats.Applicative
import cats.data.Kleisli
import cats.effect.kernel.Resource
import cats.effect.{Async, ExitCode, IO, IOApp}
import io.circe.syntax.EncoderOps
import org.http4s.CacheDirective.{`must-revalidate`, `no-cache`, `no-store`}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.headers.`Cache-Control`
import org.http4s.server.{Router, Server}
import org.http4s.{HttpRoutes, *}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object Service extends IOApp:
  def server[F[_]: Async]: Resource[F, Server] = for
    s <- BlazeServerBuilder[F]
      .bindHttp(port = 9000, "0.0.0.0")
      .withHttpApp(Service[F]().router)
      .withBanner(Nil)
      .withServiceErrorHandler(ErrorHandler[F, F])
      .withIdleTimeout(60.seconds)
      .withResponseHeaderTimeout(30.seconds)
      .resource
  yield s

  override def run(args: List[String]): IO[ExitCode] =
    server[IO].use(_ => IO.never).as(ExitCode.Success)

class Service[F[_]: Async] extends Implicits[F]:
  val noCache = `Cache-Control`(`no-cache`(), `no-store`, `must-revalidate`)

  val html = AppHtml()
  val routes = HttpRoutes.of[F] { case req @ GET -> Root =>
    ok(html.index.tags)
  }
  val router: Kleisli[F, Request[F], Response[F]] = orNotFound { Router("/" -> routes) }

  def orNotFound(rs: HttpRoutes[F]): Kleisli[F, Request[F], Response[F]] =
    Kleisli(req => rs.run(req).getOrElseF(notFound(req)))

  def notFound(req: Request[F]): F[Response[F]] =
    NotFound(Errors(s"Not found: '${req.uri}'.").asJson, noCache)

  private def ok[A](a: A)(implicit w: EntityEncoder[F, A]) = Ok(a, noCache)
