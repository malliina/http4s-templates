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
import com.malliina.app.Service.noCache
import org.http4s.Status.{InternalServerError, NotFound}
import org.http4s.server.middleware.{GZip, HSTS}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object Service extends IOApp:
  private val log = AppLogger(getClass)
  val noCache = `Cache-Control`(`no-cache`(), `no-store`, `must-revalidate`)

  log.info("Starting...")

  def server[F[_]: Async]: Resource[F, Server] = for
    s <- BlazeServerBuilder[F]
      .bindHttp(port = 9000, "0.0.0.0")
      .withHttpApp(Service[F].router)
      .withBanner(Nil)
      .withServiceErrorHandler(ErrorHandler[F].apply)
      .withIdleTimeout(60.seconds)
      .withResponseHeaderTimeout(30.seconds)
      .resource
  yield s

  override def run(args: List[String]): IO[ExitCode] =
    server[IO].use(_ => IO.never).as(ExitCode.Success)

class Service[F[_]: Async] extends BasicService[F]:
  val html = AppHtml()
  val routes = HttpRoutes.of[F] { case req @ GET -> Root =>
    ok(html.index.tags)
  }
  val static = StaticService[F](fs2.io.file.Path("public"))
  val router: Kleisli[F, Request[F], Response[F]] = gzipHsts {
    Router("/" -> routes, "/assets" -> static.routes)
  }

  def gzipHsts(rs: HttpRoutes[F]): Kleisli[F, Request[F], Response[F]] =
    GZip {
      HSTS {
        Kleisli(req => rs.run(req).getOrElseF(notFound(req)))
      }
    }

class BasicService[F[_]: Async] extends Implicits[F]:
  def ok[A](a: A)(implicit w: EntityEncoder[F, A]) =
    Ok(a, noCache)

  def notFound(req: Request[F]): F[Response[F]] =
    NotFound(Errors(s"Not found: '${req.uri}'.").asJson, noCache)

  def serverError(req: Request[F]): F[Response[F]] =
    InternalServerError(Errors(s"Server error for: '${req.uri}'.").asJson, noCache)
