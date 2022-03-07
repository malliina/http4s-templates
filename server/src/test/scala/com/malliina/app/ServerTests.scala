package com.malliina.app

import org.http4s.server.Server
import com.malliina.app.Service
import cats.effect.kernel.Resource
import cats.effect.IO
import com.malliina.app.Tokens.OpaqueToken
import com.malliina.http.FullUrl
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
    val token: OpaqueToken = OpaqueToken("a.a.a").get
    assertEquals(token.duplicate, "a.a.aa.a.a")
  }

case class ServerProps(server: Server, client: HttpClientIO):
  def port = server.address.getPort
  def baseHttpUrl: FullUrl = FullUrl("http", s"localhost:$port", "")

trait ServerSuite:
  self: CatsEffectSuite =>
  val resource: Resource[IO, ServerProps] = for
    s <- Service.emberServer[IO]
    c <- Resource.make(IO(HttpClientIO()))(c => IO(c.close()))
  yield ServerProps(s, c)
  val server = ResourceSuiteLocalFixture("server", resource)
  override def munitFixtures: Seq[Fixture[?]] = Seq(server)
