package io.scalaland.ocdquery

import doobie.implicits._
import doobie.h2.implicits._

package object example {

  val TicketRepo: Repo.EntityRepo[TicketF] =
    Repo.forEntity[TicketF](
      "tickets".tableName, {
        import com.softwaremill.quicklens._
        DefaultColumnNames.forEntity[TicketF].modify(_.from).setTo("from_".columnName)
      }
    )
}
