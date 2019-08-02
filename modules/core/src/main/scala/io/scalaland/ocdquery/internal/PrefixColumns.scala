package io.scalaland.ocdquery.internal

import io.scalaland.ocdquery.ColumnName
import magnolia._
import scala.language.experimental.macros

trait PrefixColumns[C] {

  def prepend(columns: C, prefix: String): C
}

object PrefixColumns {

  @inline def apply[C](implicit p: PrefixColumns[C]): PrefixColumns[C] = p

  type Typeclass[T] = PrefixColumns[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] =
    (columns, prefix) =>
      caseClass.construct { param =>
        param.typeclass.prepend(param.dereference(columns), prefix)
    }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  implicit def prependColumnName[A]: PrefixColumns[ColumnName[A]] =
    (column, prefix) => ColumnName[A](prefix + "." + column.name)
}
