package com.malliina.app

import cats.Applicative
import cats.data.Kleisli
import cats.effect.kernel.Resource
import cats.effect.{Async, ExitCode, IO, IOApp}
import com.comcast.ip4s.{Port, host, port}
import com.malliina.app.Service.noCache
import com.malliina.app.db.{DisabledRefDatabase, DoobieDatabase, DoobieRefDatabase, RefDatabase}
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

object Server extends IOApp:
  // SERVER_PORT is provided by Azure afaik
  val serverPort = sys.env
    .get("SERVER_PORT")
    .orElse(sys.env.get("PORT"))
    .flatMap(s => Port.fromString(s))
    .getOrElse(port"9000")

  def service[F[_]: Async]: Resource[F, Service[F]] =
    val conf = RefConf.unsafe().db
    val db: Resource[F, RefDatabase[F]] =
      if conf.enabled then DoobieDatabase.default(conf).map(runner => DoobieRefDatabase(runner))
      else Resource.pure(DisabledRefDatabase[F])
    db.map { database => Service(database) }

  def ember[F[_]: Async](svc: Service[F]): Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withHost(host"0.0.0.0")
      .withPort(serverPort)
      .withErrorHandler(ErrorHandler[F].partial)
      .withHttpApp(svc.router)
      .withRequestHeaderReceiveTimeout(30.seconds)
      .withIdleTimeout(60.seconds)
      .withShutdownTimeout(1.millis)
      .withHttp2
      .build

  def emberServer[F[_]: Async] = for
    svc <- service[F]
    srv <- ember[F](svc)
  yield srv

  override def run(args: List[String]): IO[ExitCode] =
    emberServer[IO].use(_ => IO.never).as(ExitCode.Success)
