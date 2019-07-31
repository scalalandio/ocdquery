package io.scalaland.ocdquery

import java.time.LocalDate

import cats.Id
import cats.implicits._
import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.example.{ TicketF, TicketRepo }
import monocle.macros.syntax.lens._
import org.specs2.mutable.Specification

class FetcherSpec extends Specification with WithH2Database {

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

  "Fetcher" should {

    "generate Fragments allowing you to perform basic CRUD operations" in {
      val createTicket = TicketF[Id, UnitF](
        id      = (),
        name    = "John",
        surname = "Smith",
        from    = "New York",
        to      = "London",
        date    = LocalDate.now()
      )

      import TicketRepo.meta.{ Create, Entity, Names, Select }
      type Double[A] = (A, A)
      type Triple[A] = (A, A, A)

      val join1: Fetcher[Double[Create], Double[Entity], Double[Select], Double[Names]] =
        TicketRepo.join(TicketRepo, (TicketRepo.col(_.name), TicketRepo.col(_.name)))
      val join2: Fetcher[Triple[Create], Triple[Entity], Triple[Select], Triple[Names]] =
        join1.join(TicketRepo, (_._2.name, TicketRepo.col(_.name)))

      val test: ConnectionIO[(List[Double[Entity]], List[Triple[Entity]])] = for {
        // should generate ID within SQL
        inserted <- TicketRepo.insert(createTicket).run
        _ = inserted === 1

        // should fetch duplicated entity
        byName = TicketRepo.emptySelect.lens(_.name).set(createTicket.name).lens(_.surname).set(createTicket.surname)

        result1 <- join1.fetch((byName, byName)).to[List]
        result2 <- join2.fetch((byName, byName, byName)).to[List]
      } yield result1 -> result2

      val (result1: List[Double[Entity]], result2: List[Triple[Entity]]) = test.transact(transactor).unsafeRunSync()

      result1.foreach {
        case (e1, e2) =>
          e1 === e2
      }
      result1 must not(empty)

      result2.foreach {
        case (e1, e2, e3) =>
          e1 === e2
          e2 === e3
      }
      result2 must not(empty)
    }
  }
}
