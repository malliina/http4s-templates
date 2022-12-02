package com.malliina.app

import cats.Applicative
import cats.data.Kleisli
import cats.effect.kernel.Resource
import cats.effect.{Async, ExitCode, IO, IOApp}
import com.comcast.ip4s.{host, port, Port}
import com.malliina.app.Service.noCache
import io.circe.syntax.EncoderOps
import org.http4s.CacheDirective.{`must-revalidate`, `no-cache`, `no-store`}
import org.http4s.Status.{InternalServerError, NotFound}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.`Cache-Control`
import org.http4s.server.middleware.{GZip, HSTS}
import org.http4s.server.{Router, Server}
import org.http4s.{HttpRoutes, *}
import com.malliina.app.build.BuildInfo

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object Service extends IOApp:
  private val log = AppLogger(getClass)
  val noCache = `Cache-Control`(`no-cache`(), `no-store`, `must-revalidate`)

  // SERVER_PORT is provided by Azure afaik
  val serverPort = sys.env
    .get("SERVER_PORT")
    .orElse(sys.env.get("PORT"))
    .flatMap(s => Port.fromString(s))
    .getOrElse(port"9000")

  def emberServer[F[_]: Async]: Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withHost(host"0.0.0.0")
      .withPort(serverPort)
      .withErrorHandler(ErrorHandler[F].partial)
      .withHttpApp(Service[F].router)
      .withRequestHeaderReceiveTimeout(30.seconds)
      .withIdleTimeout(60.seconds)
      .withShutdownTimeout(1.millis)
      .withHttp2
      .build

  override def run(args: List[String]): IO[ExitCode] =
    emberServer[IO].use(_ => IO.never).as(ExitCode.Success)

class Service[F[_]: Async] extends BasicService[F]:
  val html = AppHtml()
  val routes = HttpRoutes.of[F] {
    case req @ GET -> Root =>
      ok(html.index.tags)
    case req @ GET -> Root / "health" =>
      ok(BuildMeta.fromBuild.asJson)
  }
  val static = StaticService[F](BuildInfo.assetsDir, BuildInfo.assetsPath)
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

  def serverErrorAt(req: Request[F]): F[Response[F]] =
    InternalServerError(Errors(s"Server error at: '${req.uri}'.").asJson, noCache)

  def serverError: F[Response[F]] =
    InternalServerError(Errors(s"Server error.").asJson, noCache)
