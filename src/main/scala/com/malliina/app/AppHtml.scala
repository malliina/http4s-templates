package com.malliina.app

import scalatags.Text.all.*

class AppHtml:
  val empty: Modifier = ""

  def index = page("Hello!")

  def page(msg: String) = TagPage(
    html(lang := "en")(
      head(
      ),
      body(
        h1(msg)
      )
    )
  )
