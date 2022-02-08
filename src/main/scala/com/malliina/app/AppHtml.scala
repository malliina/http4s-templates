package com.malliina.app

import scalatags.Text.all.*

class AppHtml:
  val empty: Modifier = ""
  val titleTag = tag("title")

  def index = page { h1("Hello!") }

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
