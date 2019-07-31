package io.scalaland.ocdquery

import cats.Id
import cats.implicits._
import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.internal.RandomName

class Repo[C, E: Read, S, N](val meta: UnnamedRepoMeta[C, E, S, N]) { repo =>

  import meta._

  val read: Read[E] = Read[E]

  def insert(create: Create): Update0 = {
    val fragments = fromCreate(create)
    (fr"INSERT INTO" ++ table ++ fr"(" ++ fragments.asSelect ++ fr") VALUES (" ++ fragments.asValues ++ fr")").update
  }

  def fetch(select:    Select,
            sortOpt:   Option[(N => ColumnName, Repo.Sort)] = None,
            offsetOpt: Option[Long] = None,
            limitOpt:  Option[Long] = None): Query0[Entity] = {
    val orderBy = sortOpt match {
      case Some((columnf, Repo.Sort.Ascending))  => Fragment.const(s"ORDER BY ${forNames[Id](columnf)} ASC")
      case Some((columnf, Repo.Sort.Descending)) => Fragment.const(s"ORDER BY ${forNames[Id](columnf)} DESC")
      case None                                  => Fragment.empty
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

  def col(f: Names => ColumnName): Names => ColumnName = f

  def join[C1, E1, S1, N1](repo2: Repo[C1, E1, S1, N1],
                           on:    (N => ColumnName, N1 => ColumnName)*): Fetcher[(C, C1), (E, E1), (S, S1), (N, N1)] = {
    implicit val readEE1: Read[(E, E1)] = (read, repo2.read).mapN((e, e1) => e -> e1)
    new Fetcher(repo.meta.as(RandomName.next).join(repo2.meta.as(RandomName.next), on.toSeq: _*))
  }
}

object Repo {

  def apply[C, E: Read, S, N](meta: UnnamedRepoMeta[C, E, S, N]): Repo[C, E, S, N] = new Repo(meta)

  sealed trait Sort extends Product with Serializable
  object Sort {
    case object Ascending extends Sort
    case object Descending extends Sort
  }
}
