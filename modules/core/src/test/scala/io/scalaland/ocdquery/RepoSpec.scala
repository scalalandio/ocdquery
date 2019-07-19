package io.scalaland.ocdquery

import java.time.LocalDate
import java.util.UUID

import cats.Id
import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.example.{ TicketF, TicketRepo }
import org.specs2.mutable.Specification

final class RepoSpec extends Specification with WithH2Database {

  override def beforeAll(): Unit = {
    sql"""CREATE TABLE IF NOT EXISTS tickets (
            id       UUID PRIMARY KEY,
            name     TEXT NOT NULL,
            surname  TEXT NOT NULL,
            from_    TEXT NOT NULL,
            to       TEXT NOT NULL,
            date     DATE NOT NULL
          )""".update.run.transact(transactor).unsafeRunSync
    ()
  }

  val originalTicket = TicketF[Id, Id](
    id      = UUID.randomUUID(),
    name    = "John",
    surname = "Smith",
    from    = "New York",
    to      = "London",
    date    = LocalDate.now()
  )

  val expectedUpdated = TicketF[Id, Id](
    id      = originalTicket.id,
    name    = "Jane",
    surname = "Doe",
    from    = "London",
    to      = "New York",
    date    = LocalDate.now().plusDays(5) // scalastyle:ignore
  )

  val fetch = TicketF[Id, Selectable](
    id      = originalTicket.id,
    name    = Skipped,
    surname = Skipped,
    from    = Skipped,
    to      = Skipped,
    date    = Skipped
  )

  val update = TicketF[Id, Selectable](
    id      = originalTicket.id,
    name    = Fixed(expectedUpdated.name),
    surname = Fixed(expectedUpdated.surname),
    from    = Fixed(expectedUpdated.from),
    to      = Fixed(expectedUpdated.to),
    date    = Fixed(expectedUpdated.date)
  )

  "Repo" should {

    "generate Fragments allowing you to perform basic CRUD operations" in {
      val test: ConnectionIO[Int] = for {
        inserted <- TicketRepo.insert(originalTicket).run
        _ = inserted === 1
        fetchedTicket <- TicketRepo.fetch(fetch).unique
        _ = fetchedTicket === originalTicket
        updated <- TicketRepo.update(update).run
        _ = updated === 1
        updatedTicket <- TicketRepo.fetch(fetch).unique
        _ = updatedTicket === expectedUpdated
        deleted <- TicketRepo.delete(fetch).run
      } yield deleted

      test.transact(transactor).unsafeRunSync() === 1
    }
  }

}
