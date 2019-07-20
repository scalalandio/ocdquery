package io.scalaland.ocdquery

import doobie._
import doobie.implicits._

// scalastyle:off
@SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
class Repo[C, E, S](val meta: RepoMeta[C, E, S])(implicit read: Read[E]) {

  import meta._

  def insert(create: C): Update0 = {
    val fragments = fragmentsForCreate(create)
    val columns   = fragments.keys.mkString(", ")
    val values    = fragments.values.reduce(_ ++ fr", " ++ _)

    (Fragment.const(s"INSERT INTO $tableName ($columns) VALUES (") ++ values ++ fr")").update
  }

  def fetch(select: S): Query0[E] = {
    val columns = columnNames.mkString(", ")
    val where = fragmentsForSelect(select)
      .map {
        case (column, value) =>
          Fragment.const(s"$column = ") ++ value
      }
      .reduce(_ ++ fr" AND " ++ _)

    (Fragment.const(s"""SELECT $columns FROM $tableName WHERE """) ++ where).query[E]
  }

  def update(select: S, update: S): Update0 = {
    val set = fragmentsForSelect(update)
      .map {
        case (column, value) =>
          Fragment.const(s"$column = ") ++ value
      }
      .reduce(_ ++ fr", " ++ _)
    val where = fragmentsForSelect(select)
      .map {
        case (column, value) =>
          Fragment.const(s"$column = ") ++ value
      }
      .reduce(_ ++ fr" AND " ++ _)

    (Fragment.const(s"UPDATE $tableName SET ") ++ set ++ fr" WHERE " ++ where).update
  }

  def delete(select: S): Update0 = {
    val where = fragmentsForSelect(select)
      .map {
        case (column, value) =>
          Fragment.const(s"$column = ") ++ value
      }
      .reduce(_ ++ fr" AND " ++ _)

    (Fragment.const(s"""DELETE FROM $tableName WHERE """) ++ where).update
  }
}
