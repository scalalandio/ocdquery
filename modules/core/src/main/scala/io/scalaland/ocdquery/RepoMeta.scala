package io.scalaland.ocdquery

import doobie._
import io.scalaland.ocdquery.internal.{ AllColumns, FragmentsForAll, FragmentsForObligatory, FragmentsForSelectable }

import scala.collection.immutable.{ ListMap, ListSet }

trait RepoMeta[EntityF[_[_], _[_]]] {

  val tableName:             String
  val columnsNames:          ListSet[String]
  val fragmentForAll:        EntityOf[EntityF] => ListMap[String, Fragment]
  val fragmentForSelectable: SelectOf[EntityF] => ListMap[String, Fragment]
  val fragmentForObligatory: SelectOf[EntityF] => ListMap[String, Fragment]
}

object RepoMeta {

  def instanceFor[EntityF[_[_], _[_]]](
    table:   String,
    columns: ColumnsOf[EntityF]
  )(
    implicit cols: AllColumns[ColumnsOf[EntityF]],
    forAll:        FragmentsForAll[EntityOf[EntityF], ColumnsOf[EntityF]],
    forSelectable: FragmentsForSelectable[SelectOf[EntityF], ColumnsOf[EntityF]],
    forObligatory: FragmentsForObligatory[SelectOf[EntityF], ColumnsOf[EntityF]]
  ): RepoMeta[EntityF] =
    new RepoMeta[EntityF] {
      val tableName:    String          = table
      val columnsNames: ListSet[String] = ListSet(cols.getList(columns).toSeq: _*)
      val fragmentForAll: EntityOf[EntityF] => ListMap[String, Fragment] = entry =>
        ListMap(forAll.toFragments(entry, columns).toSeq: _*)
      val fragmentForSelectable: SelectOf[EntityF] => ListMap[String, Fragment] = fixed =>
        ListMap(forSelectable.toFragments(fixed, columns).toSeq: _*)
      val fragmentForObligatory: SelectOf[EntityF] => ListMap[String, Fragment] = fixed =>
        ListMap(forObligatory.toFragments(fixed, columns).toSeq: _*)
    }
}
