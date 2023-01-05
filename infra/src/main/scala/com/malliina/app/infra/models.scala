package com.malliina.app.infra

sealed abstract class Arch(val name: String):
  override def toString = name

object Arch:
  case object Arm64 extends Arch("arm64")
  case object X86_64 extends Arch("x86_64")
  case object I386 extends Arch("i386")

trait Sizes:
  def prefix: String
  def nano: String = render("nano")
  def micro: String = render("micro")
  def small: String = render("small")
  private def render(s: String) = s"$prefix.$s"

object t4g extends Sizes:
  override val prefix = "t4g"
