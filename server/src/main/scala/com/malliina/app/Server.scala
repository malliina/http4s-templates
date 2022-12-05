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

object Server extends IOApp:
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
