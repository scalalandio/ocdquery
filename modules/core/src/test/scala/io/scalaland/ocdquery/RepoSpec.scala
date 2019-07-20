package io.scalaland.ocdquery

import java.time.LocalDate

import cats.Id
import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.example.{ TicketF, TicketRepo }
import org.specs2.mutable.Specification

final class RepoSpec extends Specification with WithH2Database {

  override def beforeAll(): Unit = {
    sql"""CREATE TABLE IF NOT EXISTS tickets (
            id       UUID DEFAULT random_uuid() PRIMARY KEY,
            name     TEXT NOT NULL,
            surname  TEXT NOT NULL,
            from_    TEXT NOT NULL,
            to       TEXT NOT NULL,
            date     DATE NOT NULL
          )""".update.run.transact(transactor).unsafeRunSync
    ()
  }

  "Repo" should {

    "generate Fragments allowing you to perform basic CRUD operations" in {
      val createTicket = TicketF[Id, UnitF](
        id      = (),
        name    = "John",
        surname = "Smith",
        from    = "New York",
        to      = "London",
        date    = LocalDate.now()
      )

      val test: ConnectionIO[Option[TicketF[Id, Id]]] = for {
        // should generate ID within SQL
        inserted <- TicketRepo.insert(createTicket).run
        _ = inserted === 1

        // should fetch complete entity
        byName = TicketF[Selectable, Selectable](
          id      = Skipped,
          name    = Fixed(createTicket.name),
          surname = Fixed(createTicket.surname),
          from    = Skipped,
          to      = Skipped,
          date    = Skipped
        )
        fetchedTicket <- TicketRepo.fetch(byName).unique
        expectedTicket = TicketF[Id, Id](
          id      = fetchedTicket.id,
          name    = createTicket.name,
          surname = createTicket.surname,
          from    = createTicket.from,
          to      = createTicket.to,
          date    = createTicket.date
        )
        _ = fetchedTicket === expectedTicket

        // should update all fields but id
        byId = TicketF[Selectable, Selectable](
          id      = Fixed(fetchedTicket.id),
          name    = Skipped,
          surname = Skipped,
          from    = Skipped,
          to      = Skipped,
          date    = Skipped
        )
        expectedUpdated = TicketF[Id, Id](
          id      = fetchedTicket.id,
          name    = "Jane",
          surname = "Doe",
          from    = "London",
          to      = "New York",
          date    = LocalDate.now().plusDays(5) // scalastyle:ignore
        )
        update = TicketF[Selectable, Selectable](
          id      = Skipped,
          name    = Fixed(expectedUpdated.name),
          surname = Fixed(expectedUpdated.surname),
          from    = Fixed(expectedUpdated.from),
          to      = Fixed(expectedUpdated.to),
          date    = Fixed(expectedUpdated.date)
        )
        updated <- TicketRepo.update(byId, update).run
        _ = updated === 1
        updatedTicket <- TicketRepo.fetch(byId).unique
        _ = updatedTicket === expectedUpdated

        // should delete ticket
        deleted <- TicketRepo.delete(byId).run
        _ = deleted === 1
        deletedTicket <- TicketRepo.fetch(byId).option
      } yield deletedTicket

      test.transact(transactor).unsafeRunSync() === None
    }
  }
}
