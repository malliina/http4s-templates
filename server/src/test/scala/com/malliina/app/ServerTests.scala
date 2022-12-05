package com.malliina.app

import org.http4s.server.Server
import com.malliina.app.Service
import cats.effect.kernel.Resource
import cats.effect.IO
import com.malliina.app.Tokens.OpaqueToken
import com.malliina.http.{FullUrl, HttpClient}
import com.malliina.http.io.HttpClientIO
import munit.CatsEffectSuite

import java.util.concurrent.atomic.AtomicReference

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
