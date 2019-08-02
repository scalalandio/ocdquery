import cats.Id
import com.softwaremill.quicklens._
import doobie.h2.implicits._
import io.scalaland.ocdquery.{ AsNameOps, DefaultColumnNames, QuasiAuto, Repo }

package object example {

  val TicketRepo: Repo.EntityRepo[TicketF] = {
    // I have no idea why shapeless cannot find this Generic on its own :/
    implicit val ticketRead: doobie.Read[Repo.ForEntity[TicketF]#Entity] =
      QuasiAuto.read(shapeless.Generic[TicketF[Id, Id]])

    Repo.forEntity[TicketF](
      "tickets".tableName, {
        DefaultColumnNames.forEntity[TicketF].modify(_.from).setTo("from_".columnName)
      }
    )
  }
}
