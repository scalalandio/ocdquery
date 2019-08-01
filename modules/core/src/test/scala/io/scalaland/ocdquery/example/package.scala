package io.scalaland.ocdquery

import doobie.h2.implicits._
import io.scalaland.ocdquery._
import shapeless._

package object example {

  val entityGen = Generic[TicketF[cats.Id, cats.Id]]
  val createGen = Generic[TicketF[cats.Id, UnitF]]

  val TicketRepo: Repo.EntityRepo[TicketF] = locally {
//    implicit val entityGeneric: Generic[Repo.ForEntity[TicketF]#Entity]       = entityGen
//    implicit val createGeneric: Generic[Repo.ForEntity[TicketF]#EntityCreate] = createGen
    implicit val entityGeneric: Generic[TicketF[cats.Id, cats.Id]] = entityGen
    implicit val createGeneric: Generic[TicketF[cats.Id, UnitF]]   = createGen
    Repo.forEntity[TicketF](
      "tickets".tableName, {
        import com.softwaremill.quicklens._
        DefaultColumnNames.forEntity[TicketF].modify(_.from).setTo("from_".columnName)
      }
    )
  }
}
