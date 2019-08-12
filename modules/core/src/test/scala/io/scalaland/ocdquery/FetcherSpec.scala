package io.scalaland.ocdquery

import java.time.LocalDate

import cats.implicits.{ catsSyntaxEq => _, _ }
import doobie.implicits._
import example.{ TicketF, TicketRepo }
import io.scalaland.ocdquery.sql._
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

    val join1 = TicketRepo.join(TicketRepo).on(_._1.id, _._2.id)
    val join2 = join1.join(TicketRepo).on(_._2.id, _._3.id)

    "generate Fragments allowing you to perform basic CRUD operations" in {
      val createTicket = Create.entity[TicketF].fromTuple(("John", "Smith", "New York", "London", LocalDate.now()))

      val test = for {
        // should generate ID within SQL
        inserted <- TicketRepo.insert(createTicket).run
        _ = inserted === 1

        // should fetch duplicated entity
        result1 <- join1
          .fetch { cols =>
            (cols._1.name `=` createTicket.name) and (cols._1.surname `=` createTicket.surname) and
              (cols._2.name `=` createTicket.name) and (cols._2.surname `=` createTicket.surname)
          }
          .to[List]
        // should fetch tripled entity
        result2 <- join2
          .fetch { cols =>
            (cols._1.name `=` createTicket.name) and (cols._1.surname `=` createTicket.surname) and
              (cols._2.name `=` createTicket.name) and (cols._2.surname `=` createTicket.surname) and
              (cols._3.name `=` createTicket.name) and (cols._3.surname `=` createTicket.surname)
          }
          .to[List]
      } yield result1 -> result2

      val (result1, result2) = test.transact(transactor).unsafeRunSync()

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

    "generate valid pagination" in {
      val now = LocalDate.now()

      val names = List("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k")

      val toCreate = names.map { name =>
        Create.entity[TicketF].fromTuple((name, "TestFetcher", "TestFetcher", "TestFetcher", now))
      }

      val test = for {
        inserted <- toCreate.traverse(TicketRepo.insert(_).run).map(_.sum)
        _ = inserted === toCreate.length

        all1 <- join1
          .fetch { cols =>
            (cols._1.surname `=` "TestFetcher") and (cols._1.from `=` "TestFetcher") and (cols._1.to `=` "TestFetcher") and
              (cols._2.surname `=` "TestFetcher") and (cols._2.from `=` "TestFetcher") and (cols._2.to `=` "TestFetcher")
          }
          .to[List]
        _ = all1.map(_._1.name).toSet === names.toSet
        _ = all1.map(_._2.name).toSet === names.toSet
        all2 <- join2
          .fetch { cols =>
            (cols._1.surname `=` "TestFetcher") and (cols._1.from `=` "TestFetcher") and (cols._1.to `=` "TestFetcher") and
              (cols._2.surname `=` "TestFetcher") and (cols._2.from `=` "TestFetcher") and (cols._2.to `=` "TestFetcher") and
              (cols._3.surname `=` "TestFetcher") and (cols._3.from `=` "TestFetcher") and (cols._3.to `=` "TestFetcher")
          }
          .to[List]
        _ = all2.map(_._1.name).toSet === names.toSet
        _ = all2.map(_._2.name).toSet === names.toSet
        _ = all2.map(_._3.name).toSet === names.toSet

        count1 <- join1.count { cols =>
          (cols._1.surname `=` "TestFetcher") and (cols._1.from `=` "TestFetcher") and (cols._1.to `=` "TestFetcher") and
            (cols._2.surname `=` "TestFetcher") and (cols._2.from `=` "TestFetcher") and (cols._2.to `=` "TestFetcher")
        }.unique
        _ = count1 === names.length
        count2 <- join2.count { cols =>
          (cols._1.surname `=` "TestFetcher") and (cols._1.from `=` "TestFetcher") and (cols._1.to `=` "TestFetcher") and
            (cols._2.surname `=` "TestFetcher") and (cols._2.from `=` "TestFetcher") and (cols._2.to `=` "TestFetcher") and
            (cols._3.surname `=` "TestFetcher") and (cols._3.from `=` "TestFetcher") and (cols._3.to `=` "TestFetcher")
        }.unique
        _ = count2 === names.length
        nonEmpty1 <- join1.exists { cols =>
          (cols._1.surname `=` "TestFetcher") and (cols._1.from `=` "TestFetcher") and (cols._1.to `=` "TestFetcher") and
            (cols._2.surname `=` "TestFetcher") and (cols._2.from `=` "TestFetcher") and (cols._2.to `=` "TestFetcher")
        }.unique
        _ = nonEmpty1 === true
        nonEmpty2 <- join2.exists { cols =>
          (cols._1.surname `=` "TestFetcher") and (cols._1.from `=` "TestFetcher") and (cols._1.to `=` "TestFetcher") and
            (cols._2.surname `=` "TestFetcher") and (cols._2.from `=` "TestFetcher") and (cols._2.to `=` "TestFetcher") and
            (cols._3.surname `=` "TestFetcher") and (cols._3.from `=` "TestFetcher") and (cols._3.to `=` "TestFetcher")
        }.unique
        _ = nonEmpty2 === true

        firstHalf <- join2.fetch
          .withSort(_._1.name, Sort.Ascending)
          .withLimit(5) { cols =>
            (cols._1.surname `=` "TestFetcher") and (cols._1.from `=` "TestFetcher") and (cols._1.to `=` "TestFetcher") and
              (cols._2.surname `=` "TestFetcher") and (cols._2.from `=` "TestFetcher") and (cols._2.to `=` "TestFetcher") and
              (cols._3.surname `=` "TestFetcher") and (cols._3.from `=` "TestFetcher") and (cols._3.to `=` "TestFetcher")
          }
          .to[List]
        _ = firstHalf.map(_._1.name) === names.take(5)
        _ = firstHalf.map(_._2.name) === names.take(5)
        _ = firstHalf.map(_._3.name) === names.take(5)
        secondHalf <- join2.fetch
          .withOffset(5) { cols =>
            (cols._1.surname `=` "TestFetcher") and (cols._1.from `=` "TestFetcher") and (cols._1.to `=` "TestFetcher") and
              (cols._2.surname `=` "TestFetcher") and (cols._2.from `=` "TestFetcher") and (cols._2.to `=` "TestFetcher") and
              (cols._3.surname `=` "TestFetcher") and (cols._3.from `=` "TestFetcher") and (cols._3.to `=` "TestFetcher")
          }
          .to[List]
      } yield secondHalf.map(_._1.name)

      test.transact(transactor).unsafeRunSync() === names.drop(5)
    }
  }
}
