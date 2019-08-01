package io.scalaland.ocdquery

import java.time.LocalDate

import cats.Id
import cats.implicits._
import com.softwaremill.quicklens._
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
      val createTicket = Create
        .entity[TicketF]
        .fromTuple(
          (
            "John",
            "Smith",
            "New York",
            "London",
            LocalDate.now()
          )
        )

      val test: ConnectionIO[Option[TicketF[Id, Id]]] = for {
        // should generate ID within SQL
        inserted <- TicketRepo.insert(createTicket).run
        _ = inserted === 1

        // should fetch complete entity
        byName = TicketRepo.emptySelect
          .modify(_.name)
          .setTo(createTicket.name)
          .modify(_.surname)
          .setTo(createTicket.surname)
        fetchedTicket <- TicketRepo.fetch(byName, limit = Some(1)).unique
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
        byId = TicketRepo.emptySelect.modify(_.id).setTo(fetchedTicket.id)
        expectedUpdated = TicketF[Id, Id](
          id      = fetchedTicket.id,
          name    = "Jane",
          surname = "Doe",
          from    = "London",
          to      = "New York",
          date    = LocalDate.now().plusDays(5) // scalastyle:ignore
        )
        update = TicketRepo.emptySelect
          .modify(_.name)
          .setTo(expectedUpdated.name)
          .modify(_.surname)
          .setTo(expectedUpdated.surname)
          .modify(_.from)
          .setTo(expectedUpdated.from)
          .modify(_.to)
          .setTo(expectedUpdated.to)
          .modify(_.date)
          .setTo(expectedUpdated.date)
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
        Create.entity[TicketF].fromTuple((name, "Test", "Test", "Test", now))
      }

      val filter =
        TicketRepo.emptySelect.modify(_.surname).setTo("Test").modify(_.from).setTo("Test").modify(_.to).setTo("Test")

      val test = for {
        inserted <- toCreate.traverse(TicketRepo.insert(_).run).map(_.sum)
        _ = inserted === toCreate.length
        all <- TicketRepo.fetch(filter).to[List]
        _ = all.map(_.name).toSet === names.toSet
        firstHalf <- TicketRepo.fetch(filter, Some(TicketRepo.col(_.name) -> Sort.Ascending), None, Some(5)).to[List]
        _ = firstHalf.map(_.name) === names.take(5)
        secondHalf <- TicketRepo.fetch(filter, Some(TicketRepo.col(_.name) -> Sort.Ascending), Some(5), None).to[List]
      } yield secondHalf.map(_.name)

      test.transact(transactor).unsafeRunSync() === names.drop(5)
    }
  }
}
