package io.scalaland.ocdquery.internal

import shapeless._

trait Empty[A] {

  lazy val value: A = get()

  protected def get(): A
}

object Empty {

  def apply[A](implicit empty: Empty[A]): Empty[A] = empty

  implicit val hnilCase: Empty[HNil] = () => HNil

  implicit def hconsCase[H, T <: HList](implicit h: Empty[H], t: Empty[T]): Empty[H :: T] = () => h.get() :: t.get()

  implicit def productCase[P, Rep <: HList](implicit gen: Generic.Aux[P, Rep], empty: Empty[Rep]): Empty[P] =
    () => gen.from(empty.get())
}
