package com.malliina.app

import org.http4s.server.Server
import com.malliina.app.Service
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.malliina.http.FullUrl
import com.malliina.http.io.HttpClientIO

import java.util.concurrent.atomic.AtomicReference

class ServerTests extends munit.FunSuite with ServerSuite:
  test("make request") {
    val s = server()
    val res = s.client.get(s.baseHttpUrl).unsafeRunSync()
    assertEquals(res.code, 200)
  }

case class ServerProps(server: Server, client: HttpClientIO):
  def port = server.address.getPort
  def baseHttpUrl: FullUrl = FullUrl("http", s"localhost:$port", "")

trait ServerSuite:
  self: munit.FunSuite =>
  val server: Fixture[ServerProps] = new Fixture[ServerProps]("server"):
    private var props: Option[ServerProps] = None
    val finalizer = new AtomicReference[IO[Unit]](IO.pure(()))
    override def apply(): ServerProps = props.get
    override def beforeAll(): Unit =
      super.beforeAll()
      val (instance, closable) = Service.emberServer[IO].allocated.unsafeRunSync()
      val httpClient = HttpClientIO()
      props = Option(ServerProps(instance, httpClient))
      finalizer.set(IO(httpClient.close()) >> closable)
    override def afterAll(): Unit =
      super.afterAll()
      finalizer.get().unsafeRunSync()

  override def munitFixtures: Seq[Fixture[?]] = Seq(server)
