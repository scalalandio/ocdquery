package io.scalaland.ocdquery

import doobie._
import doobie.implicits._

package object sql {

  // scalastyle:off
  implicit class UniversalFilter[A](val columnName: ColumnName[A]) {

    def `=`(otherColumn: ColumnName[A]): Filter =
      () => Fragment.const(columnName.name) ++ fr"=" ++ Fragment.const(otherColumn.name)
    def `<>`(otherColumn: ColumnName[A]): Filter =
      () => Fragment.const(columnName.name) ++ fr"<>" ++ Fragment.const(otherColumn.name)

    def `=`(a:  A)(implicit param: Put[A]): Filter = () => Fragment.const(columnName.name) ++ fr"= $a"
    def `<>`(a: A)(implicit param: Put[A]): Filter = () => Fragment.const(columnName.name) ++ fr"<> $a"
  }
  // scalastyle:on
}
