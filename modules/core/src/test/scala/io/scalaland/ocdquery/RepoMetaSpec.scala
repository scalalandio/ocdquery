package io.scalaland.ocdquery

import java.time.LocalDate
import java.util.UUID

import cats.Id
import doobie.implicits._
import doobie.util.fragment.Fragment
import io.scalaland.ocdquery.example.TicketF
import org.specs2.mutable.Specification

final class RepoMetaSpec extends Specification with WithH2Database {

  override def beforeAll(): Unit = {
    sql"""CREATE TABLE IF NOT EXISTS tickets (
            id       UUID PRIMARY KEY,
            name     TEXT NOT NULL,
            surname: TEXT NOT NULL,
            from     TEXT NOT NULL,
            to       TEXT NOT NULL,
            date     DATE NOT NULL
          )""".update.run.transact(transactor).unsafeRunSync
    ()
  }

  val ticket = TicketF[Id, Id](
    id      = UUID.randomUUID(),
    name    = "John",
    surname = "Smith",
    from    = "New York",
    to      = "London",
    date    = LocalDate.now()
  )

  "RepoMeta" should {

    "generate Fragments with 'name = value' pairs that can be used for inserting and updating data" in {
      val fragments = example.TicketRepo.meta.fragmentForAll(ticket)

      val insertQuery = Fragment.const(
        s"INSERT INTO ${example.TicketRepo.meta.tableName} (${fragments.keys.mkString(", ")}) VALUES "
      ) ++
        fr"(" ++ fragments.values.reduce(_ ++ fr", " ++ _) ++ fr")"
      println(insertQuery) // suppress unused

      true === true // TODO
    }
  }

}
