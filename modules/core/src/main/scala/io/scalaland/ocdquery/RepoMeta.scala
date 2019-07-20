package io.scalaland.ocdquery

import cats.Id
import doobie.Fragment
import io.scalaland.ocdquery.internal._

import scala.collection.immutable.{ ListMap, ListSet }

trait RepoMeta[Create, Entity, Select] {

  val tableName:   TableName
  val columnNames: ListSet[ColumnName]

  val fragmentsForCreate: Create => ListMap[ColumnName, Fragment]
  val fragmentsForEntity: Entity => ListMap[ColumnName, Fragment]
  val fragmentsForSelect: Select => ListMap[ColumnName, Fragment]
}

object RepoMeta {

  def instant[Create, Entity, Select, Columns](
    table:   TableName,
    columns: Columns
  )(
    implicit cols: AllColumns[Columns],
    forCreate:     FragmentsForCreate[Create, Columns],
    forEntity:     FragmentsForEntity[Entity, Columns],
    forSelect:     FragmentsForSelect[Select, Columns]
  ): RepoMeta[Create, Entity, Select] =
    new RepoMeta[Create, Entity, Select] {

      val tableName:   TableName           = table
      val columnNames: ListSet[ColumnName] = ListSet(cols.getList(columns).toSeq: _*)

      val fragmentsForCreate: Create => ListMap[ColumnName, Fragment] = created =>
        ListMap(forCreate.toFragments(created, columns).toSeq: _*)
      val fragmentsForEntity: Entity => ListMap[ColumnName, Fragment] = entity =>
        ListMap(forEntity.toFragments(entity, columns).toSeq: _*)
      val fragmentsForSelect: Select => ListMap[ColumnName, Fragment] = select =>
        ListMap(forSelect.toFragments(select, columns).toSeq: _*)
    }

  def forValue[ValueF[_[_]]](
    tableName: TableName,
    columns:   ValueF[ColumnNameF]
  )(
    implicit cols: AllColumns[ValueF[ColumnNameF]],
    forCreate:     FragmentsForCreate[ValueF[Id], ValueF[ColumnNameF]],
    forEntity:     FragmentsForEntity[ValueF[Id], ValueF[ColumnNameF]],
    forSelect:     FragmentsForSelect[ValueF[Selectable], ValueF[ColumnNameF]]
  ): RepoMeta[ValueF[Id], ValueF[Id], ValueF[Selectable]] =
    instant[ValueF[Id], ValueF[Id], ValueF[Selectable], ValueF[ColumnNameF]](tableName, columns)

  def forEntity[EntityF[_[_], _[_]]](
    tableName: TableName,
    columns:   EntityF[ColumnNameF, ColumnNameF]
  )(
    implicit cols: AllColumns[EntityF[ColumnNameF, ColumnNameF]],
    forCreate:     FragmentsForCreate[EntityF[Id, UnitF], EntityF[ColumnNameF, ColumnNameF]],
    forEntity:     FragmentsForEntity[EntityF[Id, Id], EntityF[ColumnNameF, ColumnNameF]],
    forSelect:     FragmentsForSelect[EntityF[Selectable, Selectable], EntityF[ColumnNameF, ColumnNameF]]
  ): RepoMeta[EntityF[Id, UnitF], EntityF[Id, Id], EntityF[Selectable, Selectable]] =
    instant[EntityF[Id, UnitF], EntityF[Id, Id], EntityF[Selectable, Selectable], EntityF[ColumnNameF, ColumnNameF]](
      tableName,
      columns
    )
}
