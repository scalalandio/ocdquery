package io.scalaland.ocdquery

import cats.Id
import cats.implicits._
import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.internal.{ RandomName, TupleAppender }

class Fetcher[C, E: Read, S, N](val meta: NamedRepoMeta[C, E, S, N]) {

  import meta._

  val read: Read[E] = Read[E]

  def fetch(select: Select,
            sort:   Option[(N => ColumnName, Sort)] = None,
            offset: Option[Long] = None,
            limit:  Option[Long] = None): Query0[Entity] = {
    val orderBy = sort match {
      case Some((columnf, Sort.Ascending))  => Fragment.const(s"ORDER BY ${forNames[Id](columnf)} ASC")
      case Some((columnf, Sort.Descending)) => Fragment.const(s"ORDER BY ${forNames[Id](columnf)} DESC")
      case None                             => Fragment.empty
    }
    val joinOn   = joinedOn.map(fr"ON" ++ _).getOrElse(Fragment.empty)
    val where    = fr"WHERE" ++ fromSelect(select).asAnd
    val offsetFr = offset.map(offset => Fragment.const(s"OFFSET $offset")).getOrElse(Fragment.empty)
    val limitFr  = limit.map(limit => Fragment.const(s"LIMIT $limit")).getOrElse(Fragment.empty)
    (fr"SELECT" ++ * ++ fr"FROM" ++ table ++ joinOn ++ where ++ orderBy ++ offsetFr ++ limitFr).query[Entity]
  }

  def join[C1, E1, S1, N1, C0, E0, S0, N0](repo2: Repo[C1, E1, S1, N1], on: (N => ColumnName, N1 => ColumnName)*)(
    implicit
    cta: TupleAppender[C, C1, C0],
    eta: TupleAppender[E, E1, E0],
    sta: TupleAppender[S, S1, S0],
    nta: TupleAppender[N, N1, N0]
  ): Fetcher[C0, E0, S0, N0] = {
    implicit val readE0: Read[E0] = (read, repo2.read).mapN((e, e1) => eta.append(e, e1))
    new Fetcher(meta.join(repo2.meta.as(RandomName.next), on.toSeq: _*))
  }
}
