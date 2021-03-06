package io.scalaland.ocdquery

import cats.Id
import cats.implicits._
import doobie.{ Update => _, _ }
import doobie.implicits._
import io.scalaland.ocdquery.internal.{ NamedRepoMeta, RandomPrefix }
import io.scalaland.ocdquery.missingshapeless.TupleAppender

class Fetcher[Create, Entity: Read, Update, Names](val meta: NamedRepoMeta[Create, Entity, Update, Names]) {

  import meta._

  val read: Read[Entity] = Read[Entity]

  case class Fetch private[Fetcher] (sort:   Option[(Names => ColumnName[Any], Sort)] = None,
                                     offset: Option[Long]                             = None,
                                     limit:  Option[Long]                             = None) {

    def withSort[A](by: Names => ColumnName[A], direction: Sort): Fetch =
      copy(sort = Some(by.andThen(_.as[Any]) -> direction))

    def withOffset(offset: Long): Fetch = copy(offset = Some(offset))

    def withLimit(limit: Long): Fetch = copy(limit = Some(limit))

    @SuppressWarnings(Array("org.wartremover.warts.StringPlusAny"))
    def apply(filter: Names => Filter): Query0[Entity] = {
      val orderBy = sort match {
        case Some((columnf, Sort.Ascending))  => fr"ORDER BY" ++ namedColForNames[Id](columnf).fragment ++ fr"ASC"
        case Some((columnf, Sort.Descending)) => fr"ORDER BY" ++ namedColForNames[Id](columnf).fragment ++ fr"DESC"
        case None                             => Fragment.empty
      }
      val joinOn   = joinedOn.map(fr"ON" ++ _).getOrElse(Fragment.empty)
      val where    = fr"WHERE" ++ namedColForFilter(filter).fragment
      val offsetFr = offset.map(offset => Fragment.const(s"OFFSET $offset")).getOrElse(Fragment.empty)
      val limitFr  = limit.map(limit => Fragment.const(s"LIMIT $limit")).getOrElse(Fragment.empty)
      (fr"SELECT" ++ * ++ fr"FROM" ++ table ++ joinOn ++ where ++ orderBy ++ limitFr ++ offsetFr).query[Entity]
    }
  }
  def fetch: Fetch = Fetch()

  def count(filter: Names => Filter): Query0[Long] = {
    val joinOn = joinedOn.map(fr"ON" ++ _).getOrElse(Fragment.empty)
    val where  = fr"WHERE" ++ namedColForFilter(filter).fragment
    (fr"SELECT COUNT(*) FROM" ++ table ++ joinOn ++ where).query[Long]
  }

  def exists(filter: Names => Filter): Query0[Boolean] = {
    val joinOn = joinedOn.map(fr"ON" ++ _).getOrElse(Fragment.empty)
    val where  = fr"WHERE" ++ namedColForFilter(filter).fragment
    (fr"SELECT COUNT(*) > 0 FROM" ++ table ++ joinOn ++ where ++ fr"LIMIT 1").query[Boolean]
  }

  def join[C1, E1, S1, N1, F1](repo2: Repo[C1, E1, S1, N1], joinType: JoinType = JoinType.Inner)(
    implicit
    cta: TupleAppender[Create, C1],
    eta: TupleAppender[Entity, E1],
    sta: TupleAppender[Update, S1],
    nta: TupleAppender[Names, N1]
  ): Fetcher[cta.Out, eta.Out, sta.Out, nta.Out] = {
    implicit val readE0: Read[eta.Out] = (read, repo2.read).mapN((e, e1) => eta.append(e, e1))
    new Fetcher(meta.join(repo2.meta.as(RandomPrefix.next), joinType))
  }

  def on[A, B](left: Names => ColumnName[A], right: Names => ColumnName[B]): Fetcher[Create, Entity, Update, Names] =
    new Fetcher(meta.on(left.andThen(_.as[Any]), right.andThen(_.as[Any])))
}
