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

    import TicketRepo.meta.{ Create, Entity, Names, Select }
    type Double[A] = (A, A)
    type Triple[A] = (A, A, A)

    val join1: Fetcher[Double[Create], Double[Entity], Double[Select], Double[Names]] =
      TicketRepo.join(TicketRepo, (TicketRepo.col(_.id), TicketRepo.col(_.id)))
    val join2: Fetcher[Triple[Create], Triple[Entity], Triple[Select], Triple[Names]] =
      join1.join(TicketRepo, (_._2.id, TicketRepo.col(_.id)))

    "generate Fragments allowing you to perform basic CRUD operations" in {
      val createTicket = Create.entity[TicketF].fromTuple(("John", "Smith", "New York", "London", LocalDate.now()))

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

    "generate valid pagination" in {
      val now = LocalDate.now()

      val names = List("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k")

      val toCreate = names.map { name =>
        Create.entity[TicketF].fromTuple((name, "TestFetcher", "TestFetcher", "TestFetcher", now))
      }

      val filter =
        TicketRepo.emptySelect
          .modify(_.surname)
          .setTo("TestFetcher")
          .modify(_.from)
          .setTo("TestFetcher")
          .modify(_.to)
          .setTo("TestFetcher")

      val test = for {
        inserted <- toCreate.traverse(TicketRepo.insert(_).run).map(_.sum)
        _ = inserted === toCreate.length

        all1 <- join1.fetch((filter, filter)).to[List]
        _ = all1.map(_._1.name).toSet === names.toSet
        _ = all1.map(_._2.name).toSet === names.toSet
        all2 <- join2.fetch((filter, filter, filter)).to[List]
        _ = all2.map(_._1.name).toSet === names.toSet
        _ = all2.map(_._2.name).toSet === names.toSet
        _ = all2.map(_._3.name).toSet === names.toSet

        firstHalf <- join2
          .fetch((filter, filter, filter), Some(((_: Triple[Names])._1.name) -> Sort.Ascending), None, Some(5))
          .to[List]
        _ = firstHalf.map(_._1.name) === names.take(5)
        _ = firstHalf.map(_._2.name) === names.take(5)
        _ = firstHalf.map(_._3.name) === names.take(5)
        secondHalf <- join2
          .fetch((filter, filter, filter), Some(((_: Triple[Names])._1.name) -> Sort.Ascending), Some(5), None)
          .to[List]
      } yield secondHalf.map(_._1.name)

      test.transact(transactor).unsafeRunSync() === names.drop(5)
    }
  }
}
