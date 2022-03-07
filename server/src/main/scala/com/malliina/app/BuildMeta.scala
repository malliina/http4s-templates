package com.malliina.app

import com.malliina.app.build.BuildInfo
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class BuildMeta private (name: String, version: String, gitHash: String)

object BuildMeta:
  implicit val json: Codec[BuildMeta] = deriveCodec[BuildMeta]
  val fromBuild = BuildMeta(BuildInfo.name, BuildInfo.version, BuildInfo.gitHash)
