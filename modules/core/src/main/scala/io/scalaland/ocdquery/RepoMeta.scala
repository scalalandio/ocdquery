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

sealed trait UnnamedRepoMeta[C, E, S, N] extends RepoMeta[C, E, S, N] { meta =>

  def as(name: String): NamedRepoMeta[C, E, S, N] =
    new NamedRepoMeta[C, E, S, N] {

      val table:       Fragment            = meta.table ++ Fragment.const(s" AS $name")
      val columnNames: ListSet[ColumnName] = meta.columnNames.map(name + "." + _)

      val fromCreate: Create => ListMap[ColumnName, Fragment] =
        c => meta.fromCreate(c).map { case (k, v) => (name + "." + k) -> v }
      val fromEntity: Entity => ListMap[ColumnName, Fragment] =
        e => meta.fromEntity(e).map { case (k, v) => (name + "." + k) -> v }
      val fromSelect: Select => ListMap[ColumnName, Fragment] =
        s => meta.fromSelect(s).map { case (k, v) => (name + "." + k) -> v }

      def forNames[F[_]: Functor](f: Names => F[ColumnName]): F[ColumnName] =
        meta.forNames(f).map { cols =>
          if (cols.contains('.')) cols else name + "." + cols
        }

      val joinedOn: Option[Fragment] = None
    }
}

sealed trait NamedRepoMeta[C, E, S, N] extends RepoMeta[C, E, S, N] { meta0 =>

  val joinedOn: Option[Fragment]

  def join[C1, E1, S1, N1, C0, E0, S0, N0](
    meta: NamedRepoMeta[C1, E1, S1, N1],
    on:   (N => ColumnName, N1 => ColumnName)*
  )(implicit
    cta: TupleAppender[C, C1, C0],
    eta: TupleAppender[E, E1, E0],
    sta: TupleAppender[S, S1, S0],
    nta: TupleAppender[N, N1, N0]): NamedRepoMeta[C0, E0, S0, N0] =
    new NamedRepoMeta[C0, E0, S0, N0] {

      // TODO: for now only "join"
      val table:       Fragment            = meta0.table ++ Fragment.const("JOIN") ++ meta.table
      val columnNames: ListSet[ColumnName] = meta0.columnNames ++ meta.columnNames

      val fromCreate: C0 => ListMap[ColumnName, Fragment] = (cta.revert _) andThen {
        case (c, c1) => meta0.fromCreate(c) ++ meta.fromCreate(c1)
      }
      val fromEntity: E0 => ListMap[ColumnName, Fragment] = (eta.revert _) andThen {
        case (e, e1) => meta0.fromEntity(e) ++ meta.fromEntity(e1)
      }
      val fromSelect: S0 => ListMap[ColumnName, Fragment] = (sta.revert _) andThen {
        case (s, s1) => meta0.fromSelect(s) ++ meta.fromSelect(s1)
      }

      override def forNames[F[_]: Functor](f: Names => F[ColumnName]): F[ColumnName] = meta0.forNames { n =>
        meta.forNames { n1 =>
          f(nta.append(n, n1))
        }
      }

      val joinedOn: Option[Fragment] = on
        .map {
          case (nf, n1f) =>
            Fragment.const(meta0.forNames[Id](nf) + " = " + meta.forNames[Id](n1f))
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
