package com.malliina.app.db

import cats.Applicative
import com.malliina.app.AppLogger
import doobie.implicits.toSqlInterpolator

trait RefDatabase[F[_]: Applicative]:
  def version: F[String]

class DoobieRefDatabase[F[_]: Applicative](db: DatabaseRunner[F]) extends RefDatabase[F]:
  override def version: F[String] = db.run(sql"""select version()""".query[String].unique)

class DisabledRefDatabase[F[_]: Applicative] extends RefDatabase[F]:
  AppLogger(getClass).info("Database disabled.")
  override def version: F[String] = Applicative[F].pure("disabled")
