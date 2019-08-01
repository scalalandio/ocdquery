package io.scalaland.ocdquery

import cats.Id
import io.scalaland.ocdquery.internal.SkipUnit

object Create {

  def value[ValueF[_[_]]](implicit skipUnit: SkipUnit[ValueF[Id]]): Builder[skipUnit.SU, ValueF[Id]] =
    (tuple: skipUnit.SU) => skipUnit.from(tuple)

  def entity[EntityF[_[_], _[_]]](
    implicit skipUnit: SkipUnit[EntityF[Id, UnitF]]
  ): Builder[skipUnit.SU, EntityF[Id, UnitF]] =
    (tuple: skipUnit.SU) => skipUnit.from(tuple)

  trait Builder[SU, C] {

    def fromTuple(tuple: SU): C
  }
}
