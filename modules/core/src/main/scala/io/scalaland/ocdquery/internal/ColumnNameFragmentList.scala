package io.scalaland.ocdquery.internal

import doobie.util.fragment.Fragment
import io.scalaland.ocdquery.ColumnName

import scala.annotation.implicitNotFound
import scala.collection.immutable.ListMap

@implicitNotFound(
  "Couldn't find/derive ColumnNameFragmentList[${Values}, ${Names}]\n" +
    " - make sure that all fields are wrapped in obligatory or selectable F[_], " +
    "so that ${Values} is correctly substituted with ColumnName and $Names with ColumnName"
)
trait ColumnNameFragmentList[Values, Names] {
  def apply(values: Values, names: Names): List[(ColumnName[Any], Fragment)]
}

object ColumnNameFragmentList {

  implicit def combineValuesWithNames[Values, Names](
    implicit columnNameByField: ColumnNameByField[Names],
    fragmentByField:            FragmentByField[Values]
  ): ColumnNameFragmentList[Values, Names] = (values, names) => {
    val columnNames = ListMap(columnNameByField(names): _*)
    val fragments   = ListMap(fragmentByField(values):  _*)
    for {
      label <- fragments.keys.toList
      columnName <- columnNames.get(label).toList
      fragment <- fragments.get(label).toList
    } yield columnName -> fragment
  }
}
