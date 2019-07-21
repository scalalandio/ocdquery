package io.scalaland.ocdquery

import java.time.LocalDate

import cats.Id
import cats.implicits._
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

    "generate valid pagination" in {
      val now = LocalDate.now()

      val names = List("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k")

      val toCreate = names.map { name =>
        TicketF[Id, UnitF](
          id      = (),
          name    = name,
          surname = "Test",
          from    = "Test",
          to      = "Test",
          date    = now
        )
      }

      val filter = TicketF[Selectable, Selectable](
        id      = Skipped,
        name    = Skipped,
        surname = Fixed("Test"),
        from    = Fixed("Test"),
        to      = Fixed("Test"),
        date    = Skipped
      )

      val test = for {
        inserted <- toCreate.traverse(TicketRepo.insert(_).run).map(_.sum)
        _ = inserted === toCreate.length
        all <- TicketRepo.fetch(filter).to[List]
        _ = all.map(_.name).toSet === names.toSet
        firstHalf <- TicketRepo.fetch(filter, Some("name" -> Repo.Sort.Ascending), None, Some(5)).to[List]
        _ = firstHalf.map(_.name) === names.take(5)
        secondHalf <- TicketRepo.fetch(filter, Some("name" -> Repo.Sort.Ascending), Some(5), None).to[List]
      } yield secondHalf.map(_.name)

      test.transact(transactor).unsafeRunSync() === names.drop(5)
    }
  }
}
