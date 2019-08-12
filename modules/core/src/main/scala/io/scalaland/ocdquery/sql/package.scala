package io.scalaland.ocdquery

import cats.data.NonEmptyList
import doobie._
import doobie.implicits._

package object sql {

  // scalastyle:off
  implicit class UniversalSqlFilter[A](val columnName: ColumnName[A]) {

    def `=`(otherColumn: ColumnName[A]): Filter =
      () => columnName.fragment ++ fr"=" ++ otherColumn.fragment
    def <>(otherColumn: ColumnName[A]): Filter =
      () => columnName.fragment ++ fr"<>" ++ otherColumn.fragment

    // TOD: move it to a type class(?)
    def `=`(a: A)(implicit param: Put[A]): Filter = () => columnName.fragment ++ fr"= $a"
    def <>(a:  A)(implicit param: Put[A]): Filter = () => columnName.fragment ++ fr"<> $a"

    // TOD: move it to a type class(?)
    def in(values: A*)(implicit param: Put[A]): Filter =
      () =>
        values.toList match {
          case head :: tail => Fragments.in[NonEmptyList, A](columnName.fragment, NonEmptyList(head, tail))
          case Nil          => fr"true"
      }

    def between(begin: A, end: A)(implicit filterable: BetweenFilterable[A]): Filter =
      () => columnName.fragment ++ filterable.between(begin, end)
    def notBetween(begin: A, end: A)(implicit filterable: BetweenFilterable[A]): Filter =
      () => columnName.fragment ++ fr"NOT" ++ filterable.between(begin, end)

    def like(pattern: String)(implicit filterable: LikeFilterable[A]): Filter =
      () => columnName.fragment ++ filterable.like(pattern)
  }
  // scalastyle:on
}
