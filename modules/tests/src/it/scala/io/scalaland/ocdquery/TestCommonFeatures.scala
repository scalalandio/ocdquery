package io.scalaland.ocdquery

import cats.effect.{ ContextShift, IO, Resource }
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.specs2.analysisspec.IOChecker
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll

import scala.concurrent.ExecutionContext

trait TestCommonFeatures extends BeforeAfterAll with IOChecker { this: Specification =>

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

  // TODO: put here types and operations that should be supported on all databases
}
