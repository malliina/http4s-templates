package com.malliina.app

import com.malliina.http.FullUrl
import com.malliina.values.Email
import scalatags.Text.all.*

class AppHtml:
  val empty: Modifier = ""
  val titleTag = tag("title")
  val googleUrl: FullUrl = url"https://www.google.com"

  def index = page(h1("Hello!"))

  def anon = page(
    h1("Sign in"),
    a(href := "/auth/login")("Login")
  )

  def loggedIn(email: Email) = page(
    p(s"Hello, $email."),
    a(href := "/auth/switch")("Change user"),
    a(href := "/auth/logout")("Logout")
  )

  def page(bodyContent: Modifier*) = TagPage(
    html(lang := "en")(
      head(
        meta(charset := "utf-8"),
        meta(name := "viewport", content := "width=device-width, initial-scale=1.0"),
        titleTag("App"),
        link(
          rel := "shortcut icon",
          `type` := "image/jpeg",
          href := asset("/assets/kopp-small.jpg")
        )
      ),
      body(
        bodyContent
      )
    )
  )

  def asset(at: String) = at
