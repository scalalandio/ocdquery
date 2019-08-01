package io.scalaland.ocdquery.internal

import shapeless._

import scala.annotation.implicitNotFound

@implicitNotFound("Cannot found Empty[${A}]")
trait Empty[A] {

  def get(): A
}

object Empty {

  def apply[A](implicit empty: Empty[A]): Empty[A] = empty

  implicit val hnilEmpty: Empty[HNil] = () => HNil

  implicit def hconsEmpty[H, T <: HList](implicit h: Empty[H], t: Empty[T]): Empty[H :: T] = () => h.get() :: t.get()

  implicit def productEmpty[P, Rep <: HList](implicit gen: Generic.Aux[P, Rep], empty: Empty[Rep]): Empty[P] =
    () => gen.from(empty.get())
}
