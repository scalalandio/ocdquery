package io.scalaland.ocdquery

import doobie._
import doobie.implicits._

class Repo[C, E: Read, S, N](val meta: RepoMeta[C, E, S, N]) {

  import meta._

  // TODO: add handling for joins

  def insert(create: Create): Update0 = {
    val fragments = fromCreate(create)
    (fr"INSERT INTO" ++ table ++ fr"(" ++ fragments.asSelect ++ fr") VALUES (" ++ fragments.asValues ++ fr")").update
  }

  def fetch(select:    Select,
            sortOpt:   Option[(ColumnName, Repo.Sort)] = None,
            offsetOpt: Option[Long] = None,
            limitOpt:  Option[Long] = None): Query0[Entity] = {
    val orderBy = sortOpt match {
      case Some((column, Repo.Sort.Ascending))  => Fragment.const(s"ORDER BY $column ASC")
      case Some((column, Repo.Sort.Descending)) => Fragment.const(s"ORDER BY $column DESC")
      case None                                 => Fragment.empty
    }
    val offset = offsetOpt.map(offset => Fragment.const(s"OFFSET $offset")).getOrElse(Fragment.empty)
    val limit  = limitOpt.map(offset => Fragment.const(s"LIMIT $offset")).getOrElse(Fragment.empty)
    (fr"SELECT " ++ * ++ fr"FROM" ++ table ++ fr"WHERE" ++ fromSelect(select).asAnd ++ orderBy ++ offset ++ limit)
      .query[Entity]
  }

  def update(select: Select, update: Select): Update0 =
    (fr"UPDATE" ++ table ++ fr"SET" ++ fromSelect(update).asSet ++ fr"WHERE" ++ fromSelect(select).asAnd).update

  def delete(select: Select): Update0 =
    (fr"DELETE FROM" ++ table ++ fr"WHERE" ++ fromSelect(select).asAnd).update
}

object Repo {

  def apply[C, E: Read, S, N](meta: RepoMeta[C, E, S, N]): Repo[C, E, S, N] = new Repo(meta)

  sealed trait Sort extends Product with Serializable
  object Sort {
    case object Ascending extends Sort
    case object Descending extends Sort
  }
}
