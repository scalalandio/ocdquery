package io.scalaland.ocdquery.example

import doobie.h2.implicits._
import io.scalaland.ocdquery._

object TicketRepo {

  val meta = RepoMeta.instanceFor("tickets",
                                  TicketF[ColumnNameF, ColumnNameF](
                                    id      = "id",
                                    name    = "name",
                                    surname = "surname",
                                    from    = "from",
                                    to      = "to",
                                    date    = "date"
                                  ))
}
