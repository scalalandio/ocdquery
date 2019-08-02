package io.scalaland.ocdquery

import doobie._
import shapeless._

/**
  * useful bacause Shapeless apparently gets creazy when it needs to derive Generic[EntityF[Id, Id]
  */
object QuasiAuto {

  def read[A, B](gen: Generic.Aux[A, B])(implicit read: Lazy[Read[B]]): Read[A] =
    Read.generic(gen, read)
}
