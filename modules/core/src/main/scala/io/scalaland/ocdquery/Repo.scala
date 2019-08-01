package io.scalaland.ocdquery

import cats.Id
import cats.implicits._
import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.internal.{
  AllColumns,
  Empty,
  FragmentsForCreate,
  FragmentsForEntity,
  FragmentsForSelect,
  RandomName
}

class Repo[C, E: Read, S: Empty, N](val meta: UnnamedRepoMeta[C, E, S, N]) { repo =>

  import meta._

  val read: Read[E] = Read[E]

  def insert(create: Create): Update0 = {
    val fragments = fromCreate(create)
    (fr"INSERT INTO" ++ table ++ fr"(" ++ fragments.asSelect ++ fr") VALUES (" ++ fragments.asValues ++ fr")").update
  }

  def fetch(select: Select,
            sort:   Option[(N => ColumnName, Sort)] = None,
            offset: Option[Long] = None,
            limit:  Option[Long] = None): Query0[Entity] = {
    val orderBy = sort match {
      case Some((columnf, Sort.Ascending))  => Fragment.const(s"ORDER BY ${forNames[Id](columnf)} ASC")
      case Some((columnf, Sort.Descending)) => Fragment.const(s"ORDER BY ${forNames[Id](columnf)} DESC")
      case None                             => Fragment.empty
    }
    val offsetFr = offset.map(offset => Fragment.const(s"OFFSET $offset")).getOrElse(Fragment.empty)
    val limitFr  = limit.map(limit => Fragment.const(s"LIMIT $limit")).getOrElse(Fragment.empty)

    (fr"SELECT" ++ * ++ fr"FROM" ++ table ++ fr"WHERE" ++ fromSelect(select).asAnd ++ orderBy ++ offsetFr ++ limitFr)
      .query[Entity]
  }

  def update(select: Select, update: Select): Update0 =
    (fr"UPDATE" ++ table ++ fr"SET" ++ fromSelect(update).asSet ++ fr"WHERE" ++ fromSelect(select).asAnd).update

  def delete(select: Select): Update0 =
    (fr"DELETE FROM" ++ table ++ fr"WHERE" ++ fromSelect(select).asAnd).update

  def col(f: Names => ColumnName): Names => ColumnName = f

  def join[C1, E1, S1, N1](repo2: Repo[C1, E1, S1, N1],
                           on:    (N => ColumnName, N1 => ColumnName)*): Fetcher[(C, C1), (E, E1), (S, S1), (N, N1)] = {
    implicit val readEE1: Read[(E, E1)] = (read, repo2.read).mapN((e, e1) => e -> e1)
    new Fetcher(repo.meta.as(RandomName.next).join(repo2.meta.as(RandomName.next), on.toSeq: _*))
  }

  lazy val emptySelect: Select = Empty[Select].value
}

object Repo {

  def apply[C, E: Read, S: Empty, N](meta: UnnamedRepoMeta[C, E, S, N]): Repo[C, E, S, N] = new Repo(meta)

  def forValue[ValueF[_[_]]](
    tableName: TableName,
    columns:   ValueF[ColumnNameF]
  )(
    implicit cols: AllColumns[ValueF[ColumnNameF]],
    forCreate:     FragmentsForCreate[ValueF[Id], ValueF[ColumnNameF]],
    forEntity:     FragmentsForEntity[ValueF[Id], ValueF[ColumnNameF]],
    forSelect:     FragmentsForSelect[ValueF[Selectable], ValueF[ColumnNameF]],
    read:          Read[ValueF[Id]],
    emptySelect:   Empty[ValueF[Selectable]],
  ): Repo[ValueF[Id], ValueF[Id], ValueF[Selectable], ValueF[ColumnNameF]] =
    apply(RepoMeta.forValue[ValueF](tableName, columns))

  def forEntity[EntityF[_[_], _[_]]](
    tableName: TableName,
    columns:   EntityF[ColumnNameF, ColumnNameF]
  )(
    implicit cols: AllColumns[EntityF[ColumnNameF, ColumnNameF]],
    forCreate:     FragmentsForCreate[EntityF[Id, UnitF], EntityF[ColumnNameF, ColumnNameF]],
    forEntity:     FragmentsForEntity[EntityF[Id, Id], EntityF[ColumnNameF, ColumnNameF]],
    forSelect:     FragmentsForSelect[EntityF[Selectable, Selectable], EntityF[ColumnNameF, ColumnNameF]],
    read:          Read[EntityF[Id, Id]],
    emptySelect:   Empty[EntityF[Selectable, Selectable]],
  ): Repo[EntityF[Id, UnitF], EntityF[Id, Id], EntityF[Selectable, Selectable], EntityF[ColumnNameF, ColumnNameF]] =
    apply(RepoMeta.forEntity[EntityF](tableName, columns))
}
