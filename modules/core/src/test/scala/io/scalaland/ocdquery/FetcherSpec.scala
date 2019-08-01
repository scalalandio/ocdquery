package io.scalaland.ocdquery

import java.time.LocalDate

import cats.implicits._
import com.softwaremill.quicklens._
import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.example.{ TicketF, TicketRepo }
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
      val createTicket = Create.entity[TicketF].fromTuple(("John", "Smith", "New York", "London", LocalDate.now()))

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
        byName = TicketRepo.emptySelect
          .modify(_.name)
          .setTo(createTicket.name)
          .modify(_.surname)
          .setTo(createTicket.surname)

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
