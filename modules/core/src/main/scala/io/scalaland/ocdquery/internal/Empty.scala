package io.scalaland.ocdquery.internal

import io.scalaland.ocdquery.{ Skip, Updatable }
import magnolia._

import scala.language.experimental.macros
import scala.annotation.implicitNotFound

@implicitNotFound("Cannot found Empty[${A}]")
trait Empty[A] {
  def get(): A
}

object Empty {

  def apply[A](implicit empty: Empty[A]): Empty[A] = empty

  type Typeclass[T] = Empty[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] =
    () =>
      caseClass.construct { param =>
        param.typeclass.get()
    }

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  implicit def emptyUpdatable[A]: Empty[Updatable[A]] = () => Skip

  implicit val emptyUnit: Empty[Unit] = () => ()
}
