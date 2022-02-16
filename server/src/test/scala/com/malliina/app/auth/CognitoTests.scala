package com.malliina.app.auth

import cats.effect.IO
import cats.effect.kernel.Resource
import com.malliina.http.io.HttpClientIO
import com.malliina.app.Conf
import munit.CatsEffectSuite

class CognitoTests extends CatsEffectSuite:
  val httpResource = Resource.make(IO(HttpClientIO()))(c => IO(c.close()))
  val httpFixture = ResourceFixture(httpResource)
  val conf = Conf.unsafe

  httpFixture.test("token".ignore) { http =>
    val cognito = Cognito(conf, http)
    cognito.exchange("changeme").map { res =>
      println(res)
    }
  }
