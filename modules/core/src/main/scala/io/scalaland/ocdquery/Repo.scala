package io.scalaland.ocdquery

import cats.Id
import cats.implicits._
import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.internal._

class Repo[C, E: Read, U: Empty, N](val meta: UnnamedRepoMeta[C, E, U, N]) { repo =>
  import meta._

  val read: Read[Entity] = Read[Entity]

  def insert(create: Create): Update0 = {
    val fragments = fromCreate(create)
    (fr"INSERT INTO" ++ table ++ fr"(" ++ fragments.asSelect ++ fr") VALUES (" ++ fragments.asValues ++ fr")").update
  }

  case class Fetching private[Repo] (sort:   Option[(Names => ColumnName[Any], Sort)] = None,
                                     offset: Option[Long]                             = None,
                                     limit:  Option[Long]                             = None) {

    def withSort[A](by: Names => ColumnName[A], direction: Sort): Fetching =
      copy(sort = Some(by.andThen(_.as[Any]) -> direction))

    def withOffset(offset: Long): Fetching = copy(offset = Some(offset))

    def withLimit(limit: Long): Fetching = copy(limit = Some(limit))

    def apply(filter: Names => Filter): Query0[Entity] = {
      val where = unnamedColForFilter(filter).fragment
      val orderBy = sort match {
        case Some((columnf, Sort.Ascending))  => fr"ORDER BY" ++ unnamedColForNames[Id](columnf).fragment ++ fr"ASC"
        case Some((columnf, Sort.Descending)) => fr"ORDER BY" ++ unnamedColForNames[Id](columnf).fragment ++ fr"DESC"
        case None                             => Fragment.empty
      }
      val offsetFr = offset.map(offset => Fragment.const(s"OFFSET $offset")).getOrElse(Fragment.empty)
      val limitFr  = limit.map(limit => Fragment.const(s"LIMIT $limit")).getOrElse(Fragment.empty)

      (fr"SELECT" ++ * ++ fr"FROM" ++ table ++ fr"WHERE" ++ where ++ orderBy ++ offsetFr ++ limitFr).query[Entity]
    }
  }
  def fetch: Fetching = Fetching()

  def count(filter: Names => Filter): Query0[Long] = {
    val where = unnamedColForFilter(filter).fragment
    (fr"SELECT COUNT(*) FROM" ++ table ++ fr"WHERE" ++ where).query[Long]
  }

  def exists(filter: Names => Filter): Query0[Boolean] = {
    val where = unnamedColForFilter(filter).fragment
    (fr"SELECT COUNT(*) > 0 FROM" ++ table ++ fr"WHERE" ++ where ++ fr"LIMIT 1").query[Boolean]
  }

  case class Updating private[Repo] (filter: Option[Names => Filter] = None) {

    def withFilter(where: Names => Filter): Updating = copy(filter = Some(where))

    def apply(update: Update): Update0 = {
      val where = filter.map(f => fr"WHERE" ++ unnamedColForFilter(f).fragment).getOrElse(Fragment.empty)
      (fr"UPDATE" ++ table ++ fr"SET" ++ fromUpdate(update).asSet ++ where).update
    }
  }
  def update: Updating = Updating()

  def delete(filter: Names => Filter): Update0 =
    (fr"DELETE FROM" ++ table ++ fr"WHERE" ++ unnamedColForFilter(filter).fragment).update

  def join[C1, E1, S1, N1](
    repo2:    Repo[C1, E1, S1, N1],
    joinType: JoinType = JoinType.Inner
  ): Fetcher[(C, C1), (E, E1), (U, S1), (N, N1)] = {
    implicit val readEE1: Read[(E, E1)] = (read, repo2.read).mapN((e, e1) => e -> e1)
    new Fetcher(repo.meta.as(RandomPrefix.next).join(repo2.meta.as(RandomPrefix.next), joinType))
  }

  lazy val emptyUpdate: Update = Empty[Update].get()
}

object Repo {

  def apply[C, E: Read, U: Empty, N](meta: UnnamedRepoMeta[C, E, U, N]): Repo[C, E, U, N] = new Repo(meta)
  // scalastyle:off

  class ValueRepo[ValueF[_[_]]](
    meta: UnnamedRepoMeta[
      ForValue[ValueF]#ValueCreate,
      ForValue[ValueF]#Value,
      ForValue[ValueF]#ValueUpdate,
      ForValue[ValueF]#ValueColumn
    ]
  )(
    implicit valueRead: Read[ForValue[ValueF]#Value],
    emptyUpdate:        Empty[ForValue[ValueF]#ValueUpdate]
  ) extends Repo[
        ForValue[ValueF]#ValueCreate,
        ForValue[ValueF]#Value,
        ForValue[ValueF]#ValueUpdate,
        ForValue[ValueF]#ValueColumn
      ](
        meta
      ) {

    type Value       = ValueF[Id]
    type ValueCreate = ValueF[Id]
    type ValueUpdate = ValueF[Updatable]
    type ValueColumn = ValueF[ColumnName]
  }

  class ForValue[ValueF[_[_]]] {
    type Value       = ValueF[Id]
    type ValueCreate = ValueF[Id]
    type ValueUpdate = ValueF[Updatable]
    type ValueColumn = ValueF[ColumnName]

    def apply(
      tableName: TableName,
      columns:   ValueColumn
    )(
      implicit cols: AllColumns[ValueColumn],
      forEntity:     ColumnNameFragmentList[Value, ValueColumn],
      forUpdate:     ColumnNameFragmentList[ValueUpdate, ValueColumn],
      prefixColumns: PrefixColumns[ValueColumn],
      read:          Read[Value],
      emptySelect:   Empty[ValueUpdate]
    ): ValueRepo[ValueF] =
      new ValueRepo(RepoMeta.forValue[ValueF](tableName, columns))
  }
  def forValue[ValueF[_[_]]]: ForValue[ValueF] = new ForValue[ValueF]

  class EntityRepo[EntityF[_[_], _[_]]](
    meta: UnnamedRepoMeta[
      ForEntity[EntityF]#EntityCreate,
      ForEntity[EntityF]#Entity,
      ForEntity[EntityF]#EntityUpdate,
      ForEntity[EntityF]#EntityColumn
    ]
  )(
    implicit EntityRead: Read[ForEntity[EntityF]#Entity],
    emptyUpdate:         Empty[ForEntity[EntityF]#EntityUpdate]
  ) extends Repo[
        ForEntity[EntityF]#EntityCreate,
        ForEntity[EntityF]#Entity,
        ForEntity[EntityF]#EntityUpdate,
        ForEntity[EntityF]#EntityColumn
      ](
        meta
      ) {

    type Entity       = EntityF[Id, Id]
    type EntityCreate = EntityF[Id, UnitF]
    type EntityUpdate = EntityF[Updatable, Updatable]
    type EntityColumn = EntityF[ColumnName, ColumnName]
  }

  class ForEntity[EntityF[_[_], _[_]]] {
    type Entity       = EntityF[Id, Id]
    type EntityCreate = EntityF[Id, UnitF]
    type EntityUpdate = EntityF[Updatable, Updatable]
    type EntityColumn = EntityF[ColumnName, ColumnName]

    def apply(
      tableName: TableName,
      columns:   EntityColumn
    )(
      implicit cols: AllColumns[EntityColumn],
      forCreate:     ColumnNameFragmentList[EntityCreate, EntityColumn],
      forEntity:     ColumnNameFragmentList[Entity, EntityColumn],
      forUpdate:     ColumnNameFragmentList[EntityUpdate, EntityColumn],
      prefixColumns: PrefixColumns[EntityColumn],
      read:          Read[Entity],
      emptySelect:   Empty[EntityUpdate]
    ): EntityRepo[EntityF] =
      new EntityRepo[EntityF](RepoMeta.forEntity[EntityF](tableName, columns))
  }
  def forEntity[EntityF[_[_], _[_]]]: ForEntity[EntityF] = new ForEntity[EntityF]
  // scalastyle:on
}
