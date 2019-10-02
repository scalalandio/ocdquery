package io.scalaland.ocdquery

import cats.effect.{ Blocker, IO, Resource }
import doobie._
import doobie.implicits._
import doobie.hikari.HikariTransactor
import org.specs2.mutable.Specification

final class PostgresFunctionality extends Specification with TestCommonFeatures {

  protected def makeTransactor: Resource[IO, Transactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32) // scalastyle:ignore
      te <- ExecutionContexts.cachedThreadPool[IO]
      xa <- HikariTransactor.newHikariTransactor[IO](
        driverClassName = classOf[org.postgresql.Driver].getName,
        url             = "jdbc:postgresql://localhost:5432/ocdquery",
        user            = "ocdquery",
        pass            = "password",
        connectEC       = ce,
        blocker         = Blocker.liftExecutionContext(te)
      )
    } yield xa

  override protected val initSql: Seq[ConnectionIO[Int]] = super.initSql ++ Seq(
    sql"""CREATE SEQUENCE sql_entity_shortTest_seq OWNED BY sql_entity.shortTest""".update.run,
    sql"""CREATE SEQUENCE sql_entity_intTest_seq   OWNED BY sql_entity.intTest""".update.run,
    sql"""CREATE SEQUENCE sql_entity_longTest_seq  OWNED BY sql_entity.longTest""".update.run,
    sql"""ALTER TABLE sql_entity ALTER COLUMN shortTest     SET DEFAULT nextval('sql_entity_shortTest_seq')""".update.run,
    sql"""ALTER TABLE sql_entity ALTER COLUMN intTest       SET DEFAULT nextval('sql_entity_intTest_seq')""".update.run,
    sql"""ALTER TABLE sql_entity ALTER COLUMN longTest      SET DEFAULT nextval('sql_entity_longTest_seq')""".update.run,
    sql"""ALTER TABLE sql_entity ALTER COLUMN dateTest      SET DEFAULT NOW()""".update.run,
    sql"""ALTER TABLE sql_entity ALTER COLUMN timeTest      SET DEFAULT NOW()""".update.run,
    sql"""ALTER TABLE sql_entity ALTER COLUMN timestampTest SET DEFAULT NOW()""".update.run,
    sql"""ALTER TABLE sql_entity ALTER COLUMN instantTest   SET DEFAULT NOW()""".update.run,
    sql"""ALTER TABLE sql_entity ALTER COLUMN localDateTest SET DEFAULT NOW()""".update.run
  )
}
