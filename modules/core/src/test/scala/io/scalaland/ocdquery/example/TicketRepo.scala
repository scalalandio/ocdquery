package io.scalaland.ocdquery.example

import doobie.h2.implicits._
import io.scalaland.ocdquery._

object TicketRepo
    extends Repo(
      RepoMeta.forEntity("tickets",
                         TicketF[ColumnNameF, ColumnNameF](
                           id      = "id",
                           name    = "name",
                           surname = "surname",
                           from    = "from_",
                           to      = "to",
                           date    = "date"
                         ))
    )
