package io.scalaland

import cats.implicits._
import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.internal.UpdateColumns

import scala.collection.immutable.ListMap

package object ocdquery {

  type UnitF[_] = Unit

  implicit class FragmentsOps(val fragments: ListMap[ColumnName[Any], Fragment]) extends AnyVal {

    def asSelect: Fragment = fragments.keysIterator.toList.map(_.fragment).intercalate(fr",")
    def asValues: Fragment = fragments.valuesIterator.toList.intercalate(fr",")
    private def asEq = fragments.toSeq.map { case (column, value) => column.fragment ++ fr"=" ++ value }
    def asAnd: Fragment = Fragments.and(asEq: _*)
    def asOr:  Fragment = Fragments.or(asEq:  _*)
    def asSet: Fragment = Fragments.set(asEq: _*)
  }

  private val splitWordsPattern = "((^|[A-Z])([^A-Z]*))".r
  implicit class ColumnNamesOps[Names](val names: Names) extends AnyVal {

    def updateColumns(f: String => String)(implicit update: UpdateColumns[Names]): Names = update.update(names, f)

    def prefixColumns(prefix: String)(implicit update: UpdateColumns[Names]): Names = updateColumns(prefix + "." + _)

    private def splitWords(str: String) = splitWordsPattern.findAllMatchIn(str).map(_.toString).toList

    def snakeCaseColumns(implicit update: UpdateColumns[Names]): Names =
      updateColumns(str => splitWords(str).map(_.toLowerCase).mkString("_"))
  }

  implicit class AsNameOps(val name: String) extends AnyVal {

    def columnName[A]: ColumnName[A] = ColumnName(name)

    def tableName: TableName = TableName(name)
  }

  implicit def liftToUpdatable[A](a: A): Updatable[A] = Option(a).map(UpdateTo(_)).getOrElse(Skip)
}
