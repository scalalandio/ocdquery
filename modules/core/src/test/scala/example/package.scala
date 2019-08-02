import cats.Id
import doobie.h2.implicits._
import doobie.Read
import io.scalaland.ocdquery.{ AsNameOps, DefaultColumnNames, Repo }

package object example {

  val TicketRepo: Repo.EntityRepo[TicketF] = locally {

    implicit val readTicket: Read[TicketF[Id, Id]] = Helper.read

    Repo.forEntity[TicketF](
      "tickets".tableName, {
        import com.softwaremill.quicklens._
        DefaultColumnNames.forEntity[TicketF].modify(_.from).setTo("from_".columnName)
      }
    )
  }
}
