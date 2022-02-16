package com.malliina.app

import cats.Applicative
import cats.data.Kleisli
import cats.effect.kernel.Resource
import cats.effect.{Async, ExitCode, IO, IOApp}
import cats.implicits.{toFlatMapOps, toFunctorOps}
import com.comcast.ip4s.{host, port}
import com.malliina.app.Service.noCache
import com.malliina.app.auth.Cognito
import com.malliina.app.build.BuildInfo
import com.malliina.http.FullUrl
import com.malliina.http.io.HttpClientIO
import com.malliina.values.Email
import io.circe.syntax.EncoderOps
import org.http4s.CacheDirective.{`must-revalidate`, `no-cache`, `no-store`}
import org.http4s.Status.{BadRequest, InternalServerError, NotFound, SeeOther}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.`Cache-Control`
import org.http4s.server.middleware.{GZip, HSTS}
import org.http4s.server.{Router, Server}
import org.http4s.{HttpRoutes, *}
import org.http4s.headers.Location

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object Service extends IOApp:
  private val log = AppLogger(getClass)
  val noCache = `Cache-Control`(`no-cache`(), `no-store`, `must-revalidate`)

  def cognitoServer =
    for
      http <- Resource.make(IO(HttpClientIO()))(c => IO(c.close()))
      app = Service[IO](Cognito[IO](Conf.unsafe, http))
      server <- emberServer[IO](app.router)
    yield server

  def emberServer[F[_]: Async](app: HttpApp[F]) =
    EmberServerBuilder
      .default[F]
      .withHost(host"0.0.0.0")
      .withPort(port"9000")
      .withErrorHandler(ErrorHandler[F].partial)
      .withHttpApp(app)
      .withRequestHeaderReceiveTimeout(30.seconds)
      .withIdleTimeout(60.seconds)
      .withHttp2
      .build

  override def run(args: List[String]): IO[ExitCode] =
    cognitoServer.use(_ => IO.never).as(ExitCode.Success)

class Service[F[_]: Async](cognito: Cognito[F]) extends BasicService[F]:
  import Service.log
  val cookieName = "user"
  val html = AppHtml()
  val routes = HttpRoutes.of[F] {
    case req @ GET -> Root =>
      log.info(s"Cookies: ${req.cookies.length}")
      req.cookies.foreach { c =>
        log.info(s"${c.name} = ${c.content}")
      }
      req.cookies
        .find(_.name == cookieName)
        .map { userCookie =>
          ok(html.loggedIn(Email(userCookie.content)).tags)
        }
        .getOrElse {
          ok(html.anon.tags)
        }
    case req @ GET -> Root / "health" =>
      ok(BuildMeta.fromBuild.asJson)
    case req @ GET -> Root / "auth" / "login" =>
      seeOther(cognito.authorizeUrl)
    case req @ GET -> Root / "auth" / "switch" =>
      seeOther(cognito.changeUserUrl)
    case req @ GET -> Root / "auth" / "logout" =>
      seeOther(cognito.logoutUrl)
    case req @ GET -> Root / "auth" / "logout" / "callback" =>
      SeeOther(Location(Uri.unsafeFromString("/"))).map { (res: Response[F]) =>
        log.info(s"Logged out. Redirecting home.")
        res.removeCookie(userCookie(None))
      }
    case req @ GET -> Root / "auth" / "callback" =>
      log.info(s"Logging in, handling callback...")
      req.uri.query.params
        .get("code")
        .map { code =>
          cognito.email(code).flatMap { email =>
            log.info(s"Logged in as $email.")
            SeeOther(Location(Uri.unsafeFromString("/"))).map { (res: Response[F]) =>
              res.addCookie(userCookie(Option(email.value)))
            }
          }
        }
        .getOrElse {
          BadRequest(Errors("Code missing.").asJson)
        }
  }
  val static = StaticService[F](fs2.io.file.Path(BuildInfo.assetsDir))
  val router: Kleisli[F, Request[F], Response[F]] = gzipHsts {
    Router("/" -> routes, "/assets" -> static.routes)
  }

  def userCookie(content: Option[String]) = ResponseCookie(
    cookieName,
    content.getOrElse(""),
    domain = Option("localhost"),
    path = Option("/")
  )

  def gzipHsts(rs: HttpRoutes[F]): Kleisli[F, Request[F], Response[F]] =
    GZip {
      HSTS {
        Kleisli(req => rs.run(req).getOrElseF(notFound(req)))
      }
    }

class BasicService[F[_]: Async] extends Implicits[F]:
  private val log = AppLogger(getClass)

  def ok[A](a: A)(implicit w: EntityEncoder[F, A]): F[Response[F]] =
    Ok(a, noCache)

  def seeOther(url: FullUrl): F[Response[F]] =
    SeeOther(Location(Uri.unsafeFromString(url.url)))

  def notFound(req: Request[F]): F[Response[F]] =
    log.info(s"Not found: ${req.method} '${req.uri}'.")
    NotFound(Errors(s"Not found: '${req.uri}'.").asJson, noCache)

  def serverErrorAt(req: Request[F]): F[Response[F]] =
    InternalServerError(Errors(s"Server error at: '${req.uri}'.").asJson, noCache)

  def serverError: F[Response[F]] =
    InternalServerError(Errors(s"Server error.").asJson, noCache)
