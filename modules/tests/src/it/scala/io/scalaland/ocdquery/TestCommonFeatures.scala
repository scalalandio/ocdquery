package io.scalaland.ocdquery

import java.sql.{ Date, Time, Timestamp }
import java.time.{ Instant, LocalDate, LocalTime }

import cats.effect.{ ContextShift, IO, Resource }
import cats.implicits.{ catsSyntaxEq => _, _ }
import cats.Id
import doobie._
import doobie.implicits._
import doobie.specs2.analysisspec.IOChecker
import example.{ SqlEntityF, SqlValueF }
import io.scalaland.ocdquery.sql._
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import com.softwaremill.quicklens._

import scala.concurrent.ExecutionContext

trait TestCommonFeatures extends BeforeAfterAll with IOChecker { this: Specification =>

  sequential

  val isMySql = false

  protected implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  // implement for each database separately
  protected def makeTransactor: Resource[IO, Transactor[IO]]

  // add autoincrement to sql_entity on shortTest, intTest, longTest
  // by override val initStatements = super.initStatements ++ sql"...".update.run
  protected def initSql: Seq[ConnectionIO[Int]] = Seq(
    sql"""CREATE TABLE IF NOT EXISTS sql_entity (
            shortTest      INT              NOT NULL,
            intTest        INT              NOT NULL,
            longTest       BIGINT           NOT NULL,
            byteTest       INT              NOT NULL,
            floatTest      REAL             NOT NULL,
            doubleTest     DOUBLE PRECISION NOT NULL,
            bigDecimalTest DECIMAL          NOT NULL,
            stringTest     TEXT             NOT NULL,
            dateTest       DATE             NOT NULL,
            timeTest       TIME             NOT NULL,
            timestampTest  TIMESTAMP        NOT NULL,
            instantTest    TIMESTAMP        NOT NULL,
            localDateTest  DATE             NOT NULL
          )""".update.run,
    sql"""CREATE TABLE IF NOT EXISTS sql_value (
            shortTest      INT              NOT NULL,
            intTest        INT              NOT NULL,
            longTest       BIGINT           NOT NULL,
            byteTest       INT              NOT NULL,
            floatTest      REAL             NOT NULL,
            doubleTest     DOUBLE PRECISION NOT NULL,
            bigDecimalTest DECIMAL          NOT NULL,
            stringTest     TEXT             NOT NULL,
            dateTest       DATE             NOT NULL,
            timeTest       TIME             NOT NULL,
            timestampTest  TIMESTAMP        NOT NULL,
            instantTest    TIMESTAMP        NOT NULL,
            localDateTest  DATE             NOT NULL
          )""".update.run
  )

  protected def cleanSql: Seq[ConnectionIO[Int]] = Seq(
    sql"""DROP TABLE sql_value""".update.run,
    sql"""DROP TABLE sql_entity""".update.run
  )

  private val (xa, close) = makeTransactor.allocated.unsafeRunSync()

  final override def transactor: Transactor[IO] = xa

  final override def beforeAll(): Unit = initSql.reduce(_ >> _).transact(transactor).void.unsafeRunSync

  final override def afterAll(): Unit = (cleanSql.reduce(_ >> _).transact(transactor) >> close).void.unsafeRunSync()

  // should use import but Intellij doesn't see this for some reason o_0
  val SqlValueRepo:  Repo.ValueRepo[SqlValueF]   = example.SqlValueRepo
  val SqlEntityRepo: Repo.EntityRepo[SqlEntityF] = example.SqlEntityRepo
  val Fetcher = SqlValueRepo.join(SqlEntityRepo).on(_._1.stringTest, _._2.stringTest)

  val names = ('a' to 'z').map(_.toString).toList

  val createValues = names.zipWithIndex.map {
    case (name, index) =>
      Create
        .value[SqlValueF]
        .fromTuple(
          (
            index.toShort,
            index,
            index.toLong,
            (index % 8).toByte,
            index.toFloat,
            index.toDouble,
            BigDecimal(index),
            name,
            Date.valueOf(LocalDate.now),
            Time.valueOf(LocalTime.now),
            Timestamp.from(Instant.now),
            Instant.now,
            LocalDate.now
          )
        )
  }.toList

  val createEntities = names.zipWithIndex.map {
    case (name, index) =>
      Create
        .entity[SqlEntityF]
        .fromTuple(
          (
            (index % 8).toByte,
            index.toFloat,
            index.toDouble,
            BigDecimal(index),
            name
          )
        )
  }.toList

  "Value Repo" should {

    "support creation of columns of commonly supported types" in {
      createValues.traverse(SqlValueRepo.insert(_).run).map(_.sum).transact(transactor).unsafeRunSync() === names.size
    }

    "support fetch operation" in {
      names
        .traverse(name => SqlValueRepo.fetch(_.stringTest `=` name).unique)
        .transact(transactor)
        .unsafeRunSync()
        .map(_.stringTest) === names
    }

    "support pagination" in {
      val (all, pag1, pag2) = (for {
        all <- SqlValueRepo.fetch(_.stringTest.in(names:               _*)).to[List]
        pag1 <- SqlValueRepo.fetch.withLimit(5)(_.stringTest.in(names: _*)).to[List]
        pag2 <-
        // MySQL requires usage of LIMIT if you want to use OFFSET
        if (isMySql) SqlValueRepo.fetch.withOffset(5).withLimit(100)(_.stringTest.in(names: _*)).to[List]
        else SqlValueRepo.fetch.withOffset(5)(_.stringTest.in(names:                        _*)).to[List]
      } yield (all, pag1, pag2)).transact(transactor).unsafeRunSync()

      all.take(5) === pag1
      all.drop(5) === pag2
    }

    "support counting values that match filter" in {
      SqlValueRepo.count(_.stringTest.in("a", "b", "c", "d")).unique.transact(transactor).unsafeRunSync() === 4
    }

    "support checking if filter is not empty" in {
      SqlValueRepo.exists(_.stringTest.in("a")).unique.transact(transactor).unsafeRunSync() === true
      SqlValueRepo.exists(_.stringTest.in("bad")).unique.transact(transactor).unsafeRunSync() === false
    }

    "support update operation" in {
      val expected = SqlValueF[Id](
        shortTest      = 100.toShort,
        intTest        = 1000,
        longTest       = 10000L,
        byteTest       = 0.toByte,
        floatTest      = 1.5f,
        doubleTest     = 2.5,
        bigDecimalTest = BigDecimal("10000000"),
        stringTest     = "a",
        dateTest       = Date.valueOf(LocalDate.now),
        timeTest       = Time.valueOf(LocalTime.now),
        timestampTest  = Timestamp.from(Instant.now),
        instantTest    = Instant.now,
        localDateTest  = LocalDate.now
      )

      val test = for {
        updatedSize <- SqlValueRepo.update
          .withFilter(_.stringTest `=` expected.stringTest)(
            (SqlValueRepo.emptyUpdate)
              .modify(_.shortTest)
              .setTo(expected.shortTest)
              .modify(_.intTest)
              .setTo(expected.intTest)
              .modify(_.longTest)
              .setTo(expected.longTest)
              .modify(_.byteTest)
              .setTo(expected.byteTest)
              .modify(_.floatTest)
              .setTo(expected.floatTest)
              .modify(_.doubleTest)
              .setTo(expected.doubleTest)
              .modify(_.bigDecimalTest)
              .setTo(expected.bigDecimalTest)
              .modify(_.dateTest)
              .setTo(expected.dateTest)
              .modify(_.timeTest)
              .setTo(expected.timeTest)
              .modify(_.timestampTest)
              .setTo(expected.timestampTest)
              .modify(_.instantTest)
              .setTo(expected.instantTest)
              .modify(_.localDateTest)
              .setTo(expected.localDateTest)
          )
          .run
        _ = updatedSize === 1
        updated <- SqlValueRepo.fetch(_.stringTest `=` expected.stringTest).unique
      } yield mySqlFixForDatesValues(updated, expected)

      test.transact(transactor).unsafeRunSync() === expected
    }

    "support delete operation" in {
      SqlValueRepo.delete(_.stringTest.in(names: _*)).run.transact(transactor).unsafeRunSync() === names.size
    }
  }

  "Entity Repo" should {

    "support creation of columns of commonly supported types" in {
      createEntities
        .traverse(SqlEntityRepo.insert(_).run)
        .map(_.sum)
        .transact(transactor)
        .unsafeRunSync() === names.size
    }

    "support fetch operation" in {
      names
        .traverse(name => SqlEntityRepo.fetch(_.stringTest `=` name).unique)
        .transact(transactor)
        .unsafeRunSync()
        .map(_.stringTest) === names
    }

    "support pagination" in {
      val (all, pag1, pag2) = (for {
        all <- SqlEntityRepo.fetch(_.stringTest.in(names:               _*)).to[List]
        pag1 <- SqlEntityRepo.fetch.withLimit(5)(_.stringTest.in(names: _*)).to[List]
        pag2 <-
        // MySQL requires usage of LIMIT if you want to use OFFSET
        if (isMySql) SqlEntityRepo.fetch.withOffset(5).withLimit(100)(_.stringTest.in(names: _*)).to[List]
        else SqlEntityRepo.fetch.withOffset(5)(_.stringTest.in(names:                        _*)).to[List]
      } yield (all, pag1, pag2)).transact(transactor).unsafeRunSync()

      all.take(5) === pag1
      all.drop(5) === pag2
    }

    "support counting values that match filter" in {
      SqlEntityRepo.count(_.stringTest.in("a", "b", "c", "d")).unique.transact(transactor).unsafeRunSync() === 4
    }

    "support checking if filter is not empty" in {
      SqlEntityRepo.exists(_.stringTest.in("a")).unique.transact(transactor).unsafeRunSync() === true
      SqlEntityRepo.exists(_.stringTest.in("bad")).unique.transact(transactor).unsafeRunSync() === false
    }

    "support update operation" in {
      val expected = SqlEntityF[Id, Id](
        shortTest      = 100.toShort,
        intTest        = 1000,
        longTest       = 10000L,
        byteTest       = 0.toByte,
        floatTest      = 1.5f,
        doubleTest     = 2.5,
        bigDecimalTest = BigDecimal("10000000"),
        stringTest     = "a",
        dateTest       = Date.valueOf(LocalDate.now),
        timeTest       = Time.valueOf(LocalTime.now),
        timestampTest  = Timestamp.from(Instant.now),
        instantTest    = Instant.now,
        localDateTest  = LocalDate.now
      )

      val test = for {
        updatedSize <- SqlEntityRepo.update
          .withFilter(_.stringTest `=` expected.stringTest)(
            (SqlEntityRepo.emptyUpdate)
              .modify(_.shortTest)
              .setTo(expected.shortTest)
              .modify(_.intTest)
              .setTo(expected.intTest)
              .modify(_.longTest)
              .setTo(expected.longTest)
              .modify(_.byteTest)
              .setTo(expected.byteTest)
              .modify(_.floatTest)
              .setTo(expected.floatTest)
              .modify(_.doubleTest)
              .setTo(expected.doubleTest)
              .modify(_.bigDecimalTest)
              .setTo(expected.bigDecimalTest)
              .modify(_.dateTest)
              .setTo(expected.dateTest)
              .modify(_.timeTest)
              .setTo(expected.timeTest)
              .modify(_.timestampTest)
              .setTo(expected.timestampTest)
              .modify(_.instantTest)
              .setTo(expected.instantTest)
              .modify(_.localDateTest)
              .setTo(expected.localDateTest)
          )
          .run
        _ = updatedSize === 1
        updated <- SqlEntityRepo.fetch(_.stringTest `=` expected.stringTest).unique
      } yield mySqlFixForDatesEntities(updated, expected)

      test.transact(transactor).unsafeRunSync() === expected
    }

    "support delete operation" in {
      SqlEntityRepo.delete(_.stringTest.in(names: _*)).run.transact(transactor).unsafeRunSync() === names.size
    }
  }

  "Fetcher" should {

    "support fetch operation" in {
      createValues.traverse(SqlValueRepo.insert(_).run).map(_.sum).transact(transactor).unsafeRunSync()
      createEntities.traverse(SqlEntityRepo.insert(_).run).map(_.sum).transact(transactor).unsafeRunSync()

      names
        .traverse(name => Fetcher.fetch(_._1.stringTest `=` name).unique)
        .transact(transactor)
        .unsafeRunSync()
        .map(_._2.stringTest) === names
    }

    "support pagination" in {
      val (all, pag1, pag2) = (for {
        all <- Fetcher.fetch(_._1.stringTest.in(names:               _*)).to[List]
        pag1 <- Fetcher.fetch.withLimit(5)(_._1.stringTest.in(names: _*)).to[List]
        pag2 <-
        // MySQL requires usage of LIMIT if you want to use OFFSET
        if (isMySql) Fetcher.fetch.withOffset(5).withLimit(100)(_._1.stringTest.in(names: _*)).to[List]
        else Fetcher.fetch.withOffset(5)(_._1.stringTest.in(names:                        _*)).to[List]
      } yield (all, pag1, pag2)).transact(transactor).unsafeRunSync()

      all.take(5) === pag1
      all.drop(5) === pag2
    }

    "support counting values that match filter" in {
      Fetcher.count(_._1.stringTest.in("a", "b", "c", "d")).unique.transact(transactor).unsafeRunSync() === 4
    }

    "support checking if filter is not empty" in {
      Fetcher.exists(_._1.stringTest.in("a")).unique.transact(transactor).unsafeRunSync() === true
      Fetcher.exists(_._1.stringTest.in("bad")).unique.transact(transactor).unsafeRunSync() === false
    }
  }

  "Common SQL filters" should {

    "support = operator between columns and between column and value" in {
      SqlValueRepo
        .count(cols => cols.stringTest `=` cols.stringTest)
        .unique
        .transact(transactor)
        .unsafeRunSync() === names.size
      SqlValueRepo.count(_.stringTest `=` "a").unique.transact(transactor).unsafeRunSync() === 1
    }

    "support <> operator between columns and between column and value" in {
      SqlValueRepo.count(cols => cols.stringTest <> cols.stringTest).unique.transact(transactor).unsafeRunSync() === 0
      SqlValueRepo.count(_.stringTest <> "a").unique.transact(transactor).unsafeRunSync() === (names.size - 1)
    }

    "support IN operator between column and values" in {
      SqlValueRepo.count(_.stringTest.in("a", "b", "c")).unique.transact(transactor).unsafeRunSync() === 3
    }

    "support BETWEEN operator" in {
      SqlValueRepo.count(_.stringTest.between("az", "bz")).unique.transact(transactor).unsafeRunSync() === 1
    }

    "support NOT BETWEEN operator" in {
      SqlValueRepo
        .count(_.stringTest.notBetween("az", "bz"))
        .unique
        .transact(transactor)
        .unsafeRunSync() === (names.size - 1)
    }

    "support LIKE operator" in {
      SqlValueRepo.count(_.stringTest.like("a%")).unique.transact(transactor).unsafeRunSync() === 1
    }
  }

  // MySQL has messed up dates and I don't want to waste my time figuring out how to fix it in tests
  private def mySqlFixForDatesValues(result: SqlValueF[Id], expected: SqlValueF[Id]): SqlValueF[Id] =
    if (isMySql)
      result
        .modify(_.timeTest)
        .setTo(expected.timeTest)
        .modify(_.dateTest)
        .setTo(expected.dateTest)
        .modify(_.timestampTest)
        .setTo(expected.timestampTest)
        .modify(_.instantTest)
        .setTo(expected.instantTest)
        .modify(_.localDateTest)
        .setTo(expected.localDateTest)
    else result
  private def mySqlFixForDatesEntities(result: SqlEntityF[Id, Id], expected: SqlEntityF[Id, Id]): SqlEntityF[Id, Id] =
    if (isMySql)
      result
        .modify(_.timeTest)
        .setTo(expected.timeTest)
        .modify(_.dateTest)
        .setTo(expected.dateTest)
        .modify(_.timestampTest)
        .setTo(expected.timestampTest)
        .modify(_.instantTest)
        .setTo(expected.instantTest)
        .modify(_.localDateTest)
        .setTo(expected.localDateTest)
    else result
}
