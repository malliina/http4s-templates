package com.malliina.app

object Tokens:
  opaque type OpaqueToken = String

  object OpaqueToken:
    def apply(s: String): Option[OpaqueToken] = Option.when(s.count(_ == '.') == 2)(s)

  extension (t: OpaqueToken)
    def length = t.length
    def trim = t.trim
    def duplicate = s"$t$t"
