package io.scalaland.ocdquery.internal

import cats.{ Functor, Id }
import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.{ ColumnName, Filter, JoinType, TableName, UnitF, Updatable }
import io.scalaland.ocdquery.missingshapeless.TupleAppender

import scala.collection.immutable.{ ListMap, ListSet }

sealed trait RepoMeta[C, E, U, N] {
  type Create = C
  type Entity = E
  type Update = U
  type Names  = N

  val table:       Fragment
  val columnNames: ListSet[ColumnName[Any]]

  val fromCreate: Create => ListMap[ColumnName[Any], Fragment]
  val fromEntity: Entity => ListMap[ColumnName[Any], Fragment]
  val fromUpdate: Update => ListMap[ColumnName[Any], Fragment]

  lazy val * : Fragment = Fragment.const(columnNames.mkString(", "))
}

sealed trait UnnamedRepoMeta[C, E, U, N] extends RepoMeta[C, E, U, N] { meta =>

  def unnamedColForNames[F[_]: Functor](f: Names => F[ColumnName[Any]],
                                        prefix: Option[String] = None): F[ColumnName[Any]]
  def unnamedColForFilter(f:                    Names => Filter, prefix: Option[String] = None): Filter

  def as(name: String): NamedRepoMeta[C, E, U, N] =
    new NamedRepoMeta[C, E, U, N] {

      val table:       Fragment                 = meta.table ++ Fragment.const(s" AS $name")
      val columnNames: ListSet[ColumnName[Any]] = meta.columnNames.map(col => ColumnName[Any](name + "." + col.name))

      val fromCreate: Create => ListMap[ColumnName[Any], Fragment] =
        c => meta.fromCreate(c).map { case (k, v) => ColumnName[Any](name + "." + k.name) -> v }
      val fromEntity: Entity => ListMap[ColumnName[Any], Fragment] =
        e => meta.fromEntity(e).map { case (k, v) => ColumnName[Any](name + "." + k.name) -> v }
      val fromUpdate: Update => ListMap[ColumnName[Any], Fragment] =
        s => meta.fromUpdate(s).map { case (k, v) => ColumnName[Any](name + "." + k.name) -> v }

      def namedColForNames[F[_]: Functor](f: Names => F[ColumnName[Any]]): F[ColumnName[Any]] =
        meta.unnamedColForNames(f, Some(name))
      def namedColForFilter(f: Names => Filter): Filter =
        meta.unnamedColForFilter(f, Some(name))

      val joinedOn: Option[Fragment] = None
    }
}

sealed trait NamedRepoMeta[C, E, U, N] extends RepoMeta[C, E, U, N] { meta0 =>

  val joinedOn: Option[Fragment]

  def namedColForNames[F[_]: Functor](f: Names => F[ColumnName[Any]]): F[ColumnName[Any]]
  def namedColForFilter(f: Names => Filter): Filter

  def join[C1, E1, S1, N1](
    meta:     NamedRepoMeta[C1, E1, S1, N1],
    joinType: JoinType = JoinType.Inner
  )(implicit
    cta: TupleAppender[C, C1],
    eta: TupleAppender[E, E1],
    sta: TupleAppender[U, S1],
    nta: TupleAppender[N, N1]): NamedRepoMeta[cta.Out, eta.Out, sta.Out, nta.Out] =
    new NamedRepoMeta[cta.Out, eta.Out, sta.Out, nta.Out] {

      val table:       Fragment                 = meta0.table ++ joinType.fragment ++ fr"JOIN" ++ meta.table
      val columnNames: ListSet[ColumnName[Any]] = meta0.columnNames ++ meta.columnNames

      val fromCreate: cta.Out => ListMap[ColumnName[Any], Fragment] = (cta.revert _) andThen {
        case (c, c1) => meta0.fromCreate(c) ++ meta.fromCreate(c1)
      }
      val fromEntity: eta.Out => ListMap[ColumnName[Any], Fragment] = (eta.revert _) andThen {
        case (e, e1) => meta0.fromEntity(e) ++ meta.fromEntity(e1)
      }
      val fromUpdate: sta.Out => ListMap[ColumnName[Any], Fragment] = (sta.revert _) andThen {
        case (s, s1) => meta0.fromUpdate(s) ++ meta.fromUpdate(s1)
      }

      def namedColForNames[F[_]: Functor](f: Names => F[ColumnName[Any]]): F[ColumnName[Any]] =
        meta0.namedColForNames { n =>
          meta.namedColForNames { n1 =>
            f(nta.append(n, n1))
          }
        }
      def namedColForFilter(f: Names => Filter): Filter =
        meta0.namedColForFilter { n =>
          meta.namedColForFilter { n1 =>
            f(nta.append(n, n1))
          }
        }

      val joinedOn: Option[Fragment] = meta0.joinedOn
    }

  def on(left: N => ColumnName[Any], right: N => ColumnName[Any]): NamedRepoMeta[C, E, U, N] =
    new NamedRepoMeta[C, E, U, N] {

      val table:       Fragment                 = meta0.table
      val columnNames: ListSet[ColumnName[Any]] = meta0.columnNames

      val fromCreate: Create => ListMap[ColumnName[Any], Fragment] = meta0.fromCreate
      val fromEntity: Entity => ListMap[ColumnName[Any], Fragment] = meta0.fromEntity
      val fromUpdate: Update => ListMap[ColumnName[Any], Fragment] = meta0.fromUpdate

      def namedColForNames[F[_]: Functor](f: Names => F[ColumnName[Any]]): F[ColumnName[Any]] =
        meta0.namedColForNames[F](f)
      def namedColForFilter(f: Names => Filter): Filter = meta0.namedColForFilter(f)

      val joinedOn: Option[Fragment] = {
        val j = Fragment.const(namedColForNames[Id](left).name + " = " + namedColForNames[Id](right).name)
        meta0.joinedOn match {
          case Some(j0) => Some(j0 ++ fr"AND" ++ j)
          case None     => Some(j)
        }
      }
    }
}

object RepoMeta {

  def instant[Create, Entity, Select, Columns](
    tableName: TableName,
    columns:   Columns
  )(
    implicit cols: AllColumns[Columns],
    forCreate:     ColumnNameFragmentList[Create, Columns],
    forEntity:     ColumnNameFragmentList[Entity, Columns],
    forUpdate:     ColumnNameFragmentList[Select, Columns],
    prefixColumns: PrefixColumns[Columns]
  ): UnnamedRepoMeta[Create, Entity, Select, Columns] =
    new UnnamedRepoMeta[Create, Entity, Select, Columns] {

      val table:       Fragment                 = Fragment.const(tableName.name)
      val columnNames: ListSet[ColumnName[Any]] = ListSet(cols.getList(columns).toSeq: _*)

      val fromCreate: Create => ListMap[ColumnName[Any], Fragment] = created =>
        ListMap(forCreate(created, columns).toSeq: _*)
      val fromEntity: Entity => ListMap[ColumnName[Any], Fragment] = entity =>
        ListMap(forEntity(entity, columns).toSeq: _*)
      val fromUpdate: Update => ListMap[ColumnName[Any], Fragment] = select =>
        ListMap(forUpdate(select, columns).toSeq: _*)

      def unnamedColForNames[F[_]: Functor](f: Names => F[ColumnName[Any]],
                                            prefix: Option[String]): F[ColumnName[Any]] =
        f(prefix.map(prefixColumns.prepend(columns, _)).getOrElse(columns))
      def unnamedColForFilter(f: Names => Filter, prefix: Option[String]): Filter =
        f(prefix.map(prefixColumns.prepend(columns, _)).getOrElse(columns))
    }

  def forValue[ValueF[_[_]]](
    tableName: TableName,
    columns:   ValueF[ColumnName]
  )(
    implicit cols: AllColumns[ValueF[ColumnName]],
    forEntity:     ColumnNameFragmentList[ValueF[Id], ValueF[ColumnName]],
    forUpdate:     ColumnNameFragmentList[ValueF[Updatable], ValueF[ColumnName]],
    prefixColumns: PrefixColumns[ValueF[ColumnName]]
  ): UnnamedRepoMeta[ValueF[Id], ValueF[Id], ValueF[Updatable], ValueF[ColumnName]] =
    instant[ValueF[Id], ValueF[Id], ValueF[Updatable], ValueF[ColumnName]](tableName, columns)

  def forEntity[EntityF[_[_], _[_]]](
    tableName: TableName,
    columns:   EntityF[ColumnName, ColumnName]
  )(
    implicit cols: AllColumns[EntityF[ColumnName, ColumnName]],
    forCreate:     ColumnNameFragmentList[EntityF[Id, UnitF], EntityF[ColumnName, ColumnName]],
    forEntity:     ColumnNameFragmentList[EntityF[Id, Id], EntityF[ColumnName, ColumnName]],
    forUpdate:     ColumnNameFragmentList[EntityF[Updatable, Updatable], EntityF[ColumnName, ColumnName]],
    prefixColumns: PrefixColumns[EntityF[ColumnName, ColumnName]]
  ): UnnamedRepoMeta[EntityF[Id, UnitF], EntityF[Id, Id], EntityF[Updatable, Updatable], EntityF[ColumnName,
                                                                                                 ColumnName]] =
    instant[EntityF[Id, UnitF], EntityF[Id, Id], EntityF[Updatable, Updatable], EntityF[ColumnName, ColumnName]](
      tableName,
      columns
    )
}
