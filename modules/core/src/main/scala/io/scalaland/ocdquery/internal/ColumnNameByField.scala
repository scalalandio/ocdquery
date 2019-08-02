package io.scalaland.ocdquery.internal

import io.scalaland.ocdquery.ColumnName
import magnolia._

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

@implicitNotFound(
  "Couldn't find/derive ColumnNameByField[${Names}]\n" +
    " - make sure that all fields are wrapped in obligatory or selectable F[_], " +
    "so that ${Names} is made of ColumnNames only"
)
trait ColumnNameByField[Names] {
  def apply(names: Names): List[(String, ColumnName[Any])]
}

object ColumnNameByField {

  type Typeclass[T] = ColumnNameByField[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] =
    t =>
      caseClass.parameters.toList.flatMap { param =>
        param.typeclass(param.dereference(t)).map {
          case (label, columnName) =>
            if (label.isEmpty) param.label -> columnName else label -> columnName
        }
    }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  implicit def forColumnName[A]: ColumnNameByField[ColumnName[A]] = columnName => List("" -> columnName.as[Any])
}
