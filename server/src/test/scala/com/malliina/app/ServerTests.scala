package com.malliina.app

import org.http4s.server.Server
import com.malliina.app.Service
import cats.effect.kernel.Resource
import cats.effect.IO
import com.malliina.app.Tokens.OpaqueToken
import com.malliina.http.{FullUrl, HttpClient}
import com.malliina.http.io.HttpClientIO
import io.circe.{Codec, Decoder, Encoder}
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import munit.CatsEffectSuite

import java.util.concurrent.atomic.AtomicReference

trait Named:
  def name: String

enum Answer(val name: String) extends Named:
  case Maybe extends Answer("maybe")
  case Yes extends Answer("yes")
  case No extends Answer("no")

object Answer extends EnumCompanion[Answer]:
  val all = Seq(Maybe, Yes, No)

abstract class EnumCompanion[T <: Named]:
  val all: Seq[T]
  val encoder: Encoder[T] = (t: T) => t.name.asJson
  val decoder: Decoder[T] = Decoder.decodeString.emap { s =>
    all.find(_.name == s).toRight(s"Unknown input: '$s'.")
  }
  implicit val json: Codec[T] = Codec.from(decoder, encoder)

class ServerTests extends CatsEffectSuite with ServerSuite:
  test("make request") {
    val s = server()
    val req = s.client.get(s.baseHttpUrl)
    req.map(res => assertEquals(res.code, 200))
  }

  test("opaque types") {
    val token: OpaqueToken = OpaqueToken("a.a.a").toOption.get
    assertEquals(token.duplicate, "a.a.aa.a.a")
    val jwt: OpaqueToken = jwt"a.b.c"
//    val jwt2: OpaqueToken = jwt"a.b" // Doesn't compile
  }

  test("json") {
    assertEquals(Answer.Maybe.asJson.noSpaces, """"maybe"""")
    assert(decode[Answer](""""no"""").contains(Answer.No))
  }

case class ServerProps(server: Server, client: HttpClient[IO]):
  def port = server.address.getPort
  def baseHttpUrl: FullUrl = FullUrl("http", s"localhost:$port", "")

trait ServerSuite:
  self: CatsEffectSuite =>
  val resource: Resource[IO, ServerProps] = for
    s <- Server.emberServer[IO]
    c <- HttpClientIO.resource[IO]
  yield ServerProps(s, c)
  val server = ResourceSuiteLocalFixture("server", resource)
  override def munitFixtures: Seq[Fixture[?]] = Seq(server)
