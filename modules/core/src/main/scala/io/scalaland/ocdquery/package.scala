package io.scalaland

import doobie._
import doobie.implicits._

import scala.collection.immutable.ListMap

package object ocdquery {

  type TableName = String

  type ColumnName     = String
  type ColumnNameF[_] = String

  type UnitF[_] = Unit

  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  implicit class FragmentsOps(val fragments: ListMap[ColumnName, Fragment]) extends AnyVal {

    def asSelect: Fragment =
      fragments.keysIterator.map(Fragment.const(_)).reduce(_ ++ fr"," ++ _)
    def asAnd: Fragment =
      fragments.map { case (column, value) => Fragment.const(s"$column = ") ++ value }.reduce(_ ++ fr"AND" ++ _)
    def asOr: Fragment =
      fragments.map { case (column, value) => Fragment.const(s"$column = ") ++ value }.reduce(_ ++ fr"OR" ++ _)
    def asSet: Fragment =
      fragments.map { case (column, value) => Fragment.const(s"$column =") ++ value }.reduce(_ ++ fr"," ++ _)
    def asValues: Fragment =
      fragments.valuesIterator.reduce(_ ++ fr"," ++ _)
  }

  implicit def liftToSelectable[A](a: A): Selectable[A] = Option(a).map(Fixed(_)).getOrElse(Skipped)
}
