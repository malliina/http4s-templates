package com.malliina.app

import cats.data.NonEmptyList
import cats.effect.Async
import cats.implicits.*
import com.malliina.app.StaticService.log
import fs2.io.file.Path
import org.http4s.CacheDirective.{`max-age`, `no-cache`, `public`}
import org.http4s.dsl.io.GET
import org.http4s.headers.`Cache-Control`
import org.http4s.{HttpRoutes, Request, StaticFile}

import scala.concurrent.duration.DurationInt

object StaticService:
  private val log = AppLogger(getClass)

class StaticService[F[_]: Async](publicDir: Path) extends BasicService[F]:
  val fontExtensions = Seq(".woff", ".woff2", ".eot", ".ttf")
  val imageExtensions = Seq(".png", ".jpg", ".jpeg", ".ico")
  val webExtensions = Seq(".html", ".js", ".map", ".css")
  val supportedExtensions =
    webExtensions ++ fontExtensions ++ imageExtensions

  log.info(s"Reading assets from '${publicDir.absolute}'.")

  val routes = HttpRoutes.of[F] {
    case req @ GET -> rest if supportedExtensions.exists(rest.toString.endsWith) =>
      val file = rest.segments.mkString("/")
      val isCacheable = file.count(_ == '.') == 2
      val cacheHeaders =
        if isCacheable then NonEmptyList.of(`max-age`(365.days), `public`)
        else NonEmptyList.of(`no-cache`())
      log.debug(s"Searching for '$file' in '$publicDir'...")
      StaticFile
        .fromPath(publicDir.resolve(file), Option(req))
        .map(_.putHeaders(`Cache-Control`(cacheHeaders)))
        .fold(onNotFound(req))(_.pure[F])
        .flatten
  }

  private def onNotFound(req: Request[F]) =
    log.info(s"Not found '${req.uri}'.")
    notFound(req)
