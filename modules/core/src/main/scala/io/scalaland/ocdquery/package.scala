package io.scalaland

import doobie._
import doobie.implicits._

import scala.collection.immutable.ListMap

package object ocdquery {

  type UnitF[_] = Unit

  @SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
  implicit class FragmentsOps(val fragments: ListMap[ColumnName[Any], Fragment]) extends AnyVal {

    def asSelect: Fragment =
      fragments.keysIterator.map(column => Fragment.const(column.name)).reduce(_ ++ fr"," ++ _)
    def asAnd: Fragment =
      fragments.map { case (column, value) => Fragment.const(s"${column.name} = ") ++ value }.reduce(_ ++ fr"AND" ++ _)
    def asOr: Fragment =
      fragments.map { case (column, value) => Fragment.const(s"${column.name} = ") ++ value }.reduce(_ ++ fr"OR" ++ _)
    def asSet: Fragment =
      fragments.map { case (column, value) => Fragment.const(s"${column.name} =") ++ value }.reduce(_ ++ fr"," ++ _)
    def asValues: Fragment =
      fragments.valuesIterator.reduce(_ ++ fr"," ++ _)
  }

  implicit class AsNameOps(val name: String) extends AnyVal {

    def columnName[A]: ColumnName[A] = ColumnName(name)

    def tableName: TableName = TableName(name)
  }

  implicit def liftToUpdatable[A](a: A): Updatable[A] = Option(a).map(UpdateTo(_)).getOrElse(Skip)
}
