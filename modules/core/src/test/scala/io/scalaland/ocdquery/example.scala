package io.scalaland.ocdquery

import java.time.LocalDate
import java.util.UUID

import doobie.h2.implicits._

final case class TicketF[C[_], U[_]](
  id:      C[UUID],
  name:    U[String],
  surname: U[String],
  from:    U[String],
  to:      U[String],
  date:    U[LocalDate]
)

object TicketRepo {

  val tableName = "tickets"
  val columns = TicketF[RepoMeta.ColumnName, RepoMeta.ColumnName](
    id      = "id",
    name    = "name",
    surname = "surname",
    from    = "from",
    to      = "to",
    date    = "date"
  )

  val meta = RepoMeta.instanceFor(tableName, columns)
}
