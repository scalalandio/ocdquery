package io.scalaland.ocdquery.sql

import doobie.Fragment
import shapeless._

/*
 * Type can be safely put into LIKE condition
 */
trait LikeFilterable[A] {

  def like(pattern: String): Fragment
}

object LikeFilterable {

  def instant[A]: LikeFilterable[A] = pattern => Fragment.const(s"LIKE $pattern")

  implicit val likeFilterableString: LikeFilterable[String] = instant[String]

  // TODO: check if this works
  implicit def likeFilterableAnyVal[A, B: LikeFilterable](
    implicit gen: Generic.Aux[A, B :: HNil]
  ): LikeFilterable[A] = {
    gen.hashCode() // suppress unused
    instant[A]
  }
}
