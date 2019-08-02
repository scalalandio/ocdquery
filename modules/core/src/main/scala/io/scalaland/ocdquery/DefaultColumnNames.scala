package io.scalaland.ocdquery

import magnolia._

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

@implicitNotFound(
  "Couldn't find/derive DefaultColumnNames[${Names}]\n" +
    " - make sure that all fields are wrapped in obligatory or selectable F[_], " +
    "so that ${Names} is made of ColumnNames only"
)
trait DefaultColumnNames[Names] {
  def get(): Names
}

object DefaultColumnNames {

  def forValue[ValueF[_[_]]](implicit default: DefaultColumnNames[ValueF[ColumnName]]): ValueF[ColumnName] =
    default.get()

  def forEntity[EntityF[_[_], _[_]]](
    implicit default: DefaultColumnNames[EntityF[ColumnName, ColumnName]]
  ): EntityF[ColumnName, ColumnName] = default.get()

  type Typeclass[T] = DefaultColumnNames[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] =
    () =>
      caseClass.construct { param =>
        ColumnName(param.label) // should run it only for classes with only ColumnNames anyway
    }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  implicit def columnName[A]: DefaultColumnNames[ColumnName[A]] = () => ???
}
