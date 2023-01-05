package com.malliina.app

import cats.data.Kleisli
import cats.effect.Async
import cats.syntax.all.toFlatMapOps
import com.malliina.app.build.BuildInfo
import com.malliina.app.Service.noCache
import com.malliina.app.db.{DatabaseRunner, RefDatabase}
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.CacheDirective.{`must-revalidate`, `no-cache`, `no-store`}
import org.http4s.{EntityEncoder, HttpRoutes, Request, Response}
import org.http4s.headers.`Cache-Control`
import org.http4s.server.Router
import org.http4s.server.middleware.{GZip, HSTS}

object Service:
  val noCache = `Cache-Control`(`no-cache`(), `no-store`, `must-revalidate`)

class Service[F[_]: Async](database: RefDatabase[F]) extends BasicService[F]:
  val html = AppHtml()
  val routes = HttpRoutes.of[F] {
    case req @ GET -> Root =>
      ok(html.index.tags)
    case GET -> Root / "version" =>
      database.version.flatMap { version => ok(Json.obj("version" -> version.asJson)) }
    case req @ GET -> Root / "health" =>
      ok(BuildMeta.fromBuild.asJson)
  }
  val static = StaticService[F](BuildInfo.assetsDir, BuildInfo.assetsPath)
  val router: Kleisli[F, Request[F], Response[F]] = gzipHsts {
    Router("/" -> routes, "/assets" -> static.routes)
  }

class BasicService[F[_]: Async] extends Implicits[F]:
  def gzipHsts(rs: HttpRoutes[F]): Kleisli[F, Request[F], Response[F]] =
    GZip {
      HSTS {
        Kleisli(req => rs.run(req).getOrElseF(notFound(req)))
      }
    }

  def ok[A](a: A)(implicit w: EntityEncoder[F, A]) =
    Ok(a, noCache)

  def notFound(req: Request[F]): F[Response[F]] =
    NotFound(Errors(s"Not found: '${req.uri}'.").asJson, noCache)

  def serverErrorAt(req: Request[F]): F[Response[F]] =
    InternalServerError(Errors(s"Server error at: '${req.uri}'.").asJson, noCache)

  def serverError: F[Response[F]] =
    InternalServerError(Errors(s"Server error.").asJson, noCache)
