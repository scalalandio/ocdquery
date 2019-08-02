package io.scalaland.ocdquery.internal

import io.scalaland.ocdquery.ColumnName

import scala.annotation.implicitNotFound

@implicitNotFound(
  "Couldn't find/derive AllColumns[${Names}]\n" +
    " - make sure that all fields are wrapped in obligatory or selectable F[_], " +
    "so that ${Names} is made of Strings only"
)
trait AllColumns[Names] {
  def getList(names: Names): List[ColumnName[Any]]
}

object AllColumns {

  implicit def dropLabel[Names](implicit columnNameByField: ColumnNameByField[Names]): AllColumns[Names] =
    names => columnNameByField(names).map(_._2)
}
