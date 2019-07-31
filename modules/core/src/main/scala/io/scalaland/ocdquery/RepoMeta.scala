package io.scalaland.ocdquery

import cats.{ Functor, Id }
import cats.syntax.functor._
import doobie.Fragment
import io.scalaland.ocdquery.internal._

import scala.collection.immutable.{ ListMap, ListSet }

sealed trait RepoMeta[C, E, S, N] {
  type Create = C
  type Entity = E
  type Select = S
  type Names  = N

  val table:       Fragment
  val columnNames: ListSet[ColumnName]

  val fromCreate: Create => ListMap[ColumnName, Fragment]
  val fromEntity: Entity => ListMap[ColumnName, Fragment]
  val fromSelect: Select => ListMap[ColumnName, Fragment]

  def forNames[F[_]: Functor](f: Names => F[ColumnName]): F[ColumnName]

  lazy val * : Fragment = Fragment.const(columnNames.mkString(", "))
}

sealed trait UnnamedRepoMeta[C, E, S, N] extends RepoMeta[C, E, S, N] { repo =>

  def as(name: String): NamedRepoMeta[C, E, S, N] =
    new NamedRepoMeta[C, E, S, N] {

      val table:       Fragment            = repo.table ++ Fragment.const(s" AS $name")
      val columnNames: ListSet[ColumnName] = repo.columnNames.map(name + "." + _)

      val fromCreate: Create => ListMap[ColumnName, Fragment] =
        c => repo.fromCreate(c).map { case (k, v) => (name + "." + k) -> v }
      val fromEntity: Entity => ListMap[ColumnName, Fragment] =
        e => repo.fromEntity(e).map { case (k, v) => (name + "." + k) -> v }
      val fromSelect: Select => ListMap[ColumnName, Fragment] =
        s => repo.fromSelect(s).map { case (k, v) => (name + "." + k) -> v }

      def forNames[F[_]: Functor](f: Names => F[ColumnName]): F[ColumnName] = repo.forNames(f).map("." + _)

      val joinOn: Option[Fragment] = None
    }
}

sealed trait NamedRepoMeta[C, E, S, N] extends RepoMeta[C, E, S, N] { repo1 =>

  val joinOn: Option[Fragment]

  def join[C1, E1, S1, N1](
    repo2: NamedRepoMeta[C1, E1, S1, N1],
    on:    (N => ColumnName, N1 => ColumnName)*
  ): NamedRepoMeta[(C, C1), (E, E1), (S, S1), (N, N1)] =
    new NamedRepoMeta[(C, C1), (E, E1), (S, S1), (N, N1)] {

      // TODO: for now only join
      val table:       Fragment            = repo1.table ++ Fragment.const(" JOIN ") ++ repo2.table
      val columnNames: ListSet[ColumnName] = repo1.columnNames ++ repo1.columnNames

      val fromCreate: ((C, C1)) => ListMap[ColumnName, Fragment] = {
        case (c, c1) => repo1.fromCreate(c) ++ repo2.fromCreate(c1)
      }
      val fromEntity: ((E, E1)) => ListMap[ColumnName, Fragment] = {
        case (e, e1) => repo1.fromEntity(e) ++ repo2.fromEntity(e1)
      }
      val fromSelect: ((S, S1)) => ListMap[ColumnName, Fragment] = {
        case (s, s1) => repo1.fromSelect(s) ++ repo2.fromSelect(s1)
      }

      def forNames[F[_]: Functor](f: Names => F[ColumnName]): F[ColumnName] = repo1.forNames { n =>
        repo2.forNames { n1 =>
          f((n, n1))
        }
      }

      val joinOn: Option[Fragment] = on
        .map {
          case (nf, n1f) =>
            Fragment.const(repo1.forNames[Id](nf) + " = " + repo2.forNames[Id](n1f))
        }
        .reduceOption(_ ++ Fragment.const("AND") ++ _)
    }
}

object RepoMeta {

  def instant[Create, Entity, Select, Columns](
    tableName: TableName,
    columns:   Columns
  )(
    implicit cols: AllColumns[Columns],
    forCreate:     FragmentsForCreate[Create, Columns],
    forEntity:     FragmentsForEntity[Entity, Columns],
    forSelect:     FragmentsForSelect[Select, Columns]
  ): UnnamedRepoMeta[Create, Entity, Select, Columns] =
    new UnnamedRepoMeta[Create, Entity, Select, Columns] {

      val table:       Fragment            = Fragment.const(tableName)
      val columnNames: ListSet[ColumnName] = ListSet(cols.getList(columns).toSeq: _*)

      val fromCreate: Create => ListMap[ColumnName, Fragment] = created =>
        ListMap(forCreate.toFragments(created, columns).toSeq: _*)
      val fromEntity: Entity => ListMap[ColumnName, Fragment] = entity =>
        ListMap(forEntity.toFragments(entity, columns).toSeq: _*)
      val fromSelect: Select => ListMap[ColumnName, Fragment] = select =>
        ListMap(forSelect.toFragments(select, columns).toSeq: _*)

      def forNames[F[_]: Functor](f: Names => F[ColumnName]): F[ColumnName] = f(columns)
    }

  def forValue[ValueF[_[_]]](
    tableName: TableName,
    columns:   ValueF[ColumnNameF]
  )(
    implicit cols: AllColumns[ValueF[ColumnNameF]],
    forCreate:     FragmentsForCreate[ValueF[Id], ValueF[ColumnNameF]],
    forEntity:     FragmentsForEntity[ValueF[Id], ValueF[ColumnNameF]],
    forSelect:     FragmentsForSelect[ValueF[Selectable], ValueF[ColumnNameF]]
  ): UnnamedRepoMeta[ValueF[Id], ValueF[Id], ValueF[Selectable], ValueF[ColumnNameF]] =
    instant[ValueF[Id], ValueF[Id], ValueF[Selectable], ValueF[ColumnNameF]](tableName, columns)

  def forEntity[EntityF[_[_], _[_]]](
    tableName: TableName,
    columns:   EntityF[ColumnNameF, ColumnNameF]
  )(
    implicit cols: AllColumns[EntityF[ColumnNameF, ColumnNameF]],
    forCreate:     FragmentsForCreate[EntityF[Id, UnitF], EntityF[ColumnNameF, ColumnNameF]],
    forEntity:     FragmentsForEntity[EntityF[Id, Id], EntityF[ColumnNameF, ColumnNameF]],
    forSelect:     FragmentsForSelect[EntityF[Selectable, Selectable], EntityF[ColumnNameF, ColumnNameF]]
  ): UnnamedRepoMeta[EntityF[Id, UnitF], EntityF[Id, Id], EntityF[Selectable, Selectable], EntityF[ColumnNameF,
                                                                                                   ColumnNameF]] =
    instant[EntityF[Id, UnitF], EntityF[Id, Id], EntityF[Selectable, Selectable], EntityF[ColumnNameF, ColumnNameF]](
      tableName,
      columns
    )
}
