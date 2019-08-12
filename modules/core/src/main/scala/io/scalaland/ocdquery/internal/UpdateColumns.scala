package io.scalaland.ocdquery.internal

import io.scalaland.ocdquery.ColumnName
import magnolia._

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

@implicitNotFound(
  "Couldn't find/derive UpdateColumns[${Names}]\n" +
    " - make sure that all fields are wrapped in obligatory or selectable F[_], " +
    "so that ${Names} is correctly substituted with ColumnName"
)
trait UpdateColumns[Names] {

  def update(columns: Names, f: String => String): Names
}

object UpdateColumns {

  @inline def apply[C](implicit p: UpdateColumns[C]): UpdateColumns[C] = p

  type Typeclass[T] = UpdateColumns[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] =
    (columns, f) =>
      caseClass.construct { param =>
        param.typeclass.update(param.dereference(columns), f)
    }

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  implicit def prependColumnName[A]: UpdateColumns[ColumnName[A]] =
    (column, f) => ColumnName[A](f(column.name))
}
