package io.scalaland.ocdquery

import doobie._
import doobie.implicits._

@SuppressWarnings(Array("org.wartremover.warts.TraversableOps"))
class Repo[EntityF[_[_], _[_]]](val meta: RepoMeta[EntityF])(implicit read: Read[EntityOf[EntityF]]) {

  import meta._

  def insert(entity: EntityOf[EntityF]): Update0 = {
    val fragments = fragmentForAll(entity)
    val columns   = fragments.keys.mkString(", ")
    val values    = fragments.values.reduce(_ ++ fr", " ++ _)

    (Fragment.const(s"INSERT INTO $tableName ($columns) VALUES (") ++ values ++ fr")").update
  }

  def fetch(select: SelectOf[EntityF]): Query0[EntityOf[EntityF]] = {
    val columns = columnsNames.mkString(", ")
    val where = (fragmentForObligatory(select) ++ fragmentForSelectable(select))
      .map {
        case (column, value) =>
          Fragment.const(s"$column = ") ++ value
      }
      .reduce(_ ++ fr" AND " ++ _)

    (Fragment.const(s"""SELECT $columns FROM $tableName WHERE """) ++ where).query[EntityOf[EntityF]]
  }

  def update(select: SelectOf[EntityF]): Update0 = {
    val update = fragmentForSelectable(select)
      .map {
        case (column, value) =>
          Fragment.const(s"$column = ") ++ value
      }
      .reduce(_ ++ fr", " ++ _)
    val where = fragmentForObligatory(select)
      .map {
        case (column, value) =>
          Fragment.const(s"$column = ") ++ value
      }
      .reduce(_ ++ fr" AND " ++ _)

    (Fragment.const(s"UPDATE $tableName SET ") ++ update ++ fr" WHERE " ++ where).update
  }

  def delete(select: SelectOf[EntityF]): Update0 = {
    val where = (fragmentForObligatory(select) ++ fragmentForSelectable(select))
      .map {
        case (column, value) =>
          Fragment.const(s"$column = ") ++ value
      }
      .reduce(_ ++ fr" AND " ++ _)

    (Fragment.const(s"""DELETE FROM $tableName WHERE """) ++ where).update
  }
}
