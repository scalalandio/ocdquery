package io.scalaland.ocdquery

import doobie._
import shapeless._

import scala.collection.immutable.ListMap

trait RepoMeta[EntityF[_[_], _[_]]] {

  val tableName:             String
  val fragmentForAll:        EntityOf[EntityF] => ListMap[String, Fragment]
  val fragmentForSelectable: SelectOf[EntityF] => ListMap[String, Fragment]
  val fragmentForObligatory: SelectOf[EntityF] => ListMap[String, Fragment]
}

object RepoMeta {

  def instanceFor[EntityF[_[_], _[_]]](
    table:   String,
    columns: EntityF[ColumnNameF, ColumnNameF]
  )(
    implicit forAll: FragmentForAll[EntityF[Id, Id], EntityF[ColumnNameF, ColumnNameF]],
    forSelectable:   FragmentForSelectable[EntityF[Id, Selectable], EntityF[ColumnNameF, ColumnNameF]],
    forObligatory:   FragmentForObligatory[EntityF[Id, Selectable], EntityF[ColumnNameF, ColumnNameF]]
  ): RepoMeta[EntityF] =
    new RepoMeta[EntityF] {
      val tableName: String = table
      val fragmentForAll: EntityOf[EntityF] => ListMap[String, Fragment] = entry =>
        ListMap(forAll.toFragment(entry, columns).toSeq: _*)
      val fragmentForSelectable: SelectOf[EntityF] => ListMap[String, Fragment] = fixed =>
        ListMap(forSelectable.toFragment(fixed, columns).toSeq: _*)
      val fragmentForObligatory: SelectOf[EntityF] => ListMap[String, Fragment] = fixed =>
        ListMap(forObligatory.toFragment(fixed, columns).toSeq: _*)
    }
}
