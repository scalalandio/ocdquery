package io.scalaland.ocdquery

import cats.effect.{ Blocker, IO, Resource }
import doobie._
import doobie.implicits._
import doobie.h2.H2Transactor
import org.specs2.mutable.Specification

final class H2Functionality extends Specification with TestCommonFeatures {

  protected def makeTransactor: Resource[IO, doobie.Transactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32) // scalastyle:ignore
      te <- ExecutionContexts.cachedThreadPool[IO]
      xa <- H2Transactor.newH2Transactor[IO](
        url       = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user      = "ocdquery",
        pass      = "password",
        connectEC = ce,
        blocker   = Blocker.liftExecutionContext(te)
      )
    } yield xa

  override protected val initSql: Seq[ConnectionIO[Int]] = super.initSql ++ Seq(
    sql"""ALTER TABLE sql_entity ALTER COLUMN shortTest     INT       NOT NULL AUTO_INCREMENT""".update.run,
    sql"""ALTER TABLE sql_entity ALTER COLUMN intTest       INT       NOT NULL AUTO_INCREMENT""".update.run,
    sql"""ALTER TABLE sql_entity ALTER COLUMN longTest      BIGINT    NOT NULL AUTO_INCREMENT""".update.run,
    sql"""ALTER TABLE sql_entity ALTER COLUMN dateTest      DATE      NOT NULL DEFAULT CURRENT_DATE""".update.run,
    sql"""ALTER TABLE sql_entity ALTER COLUMN timeTest      TIME      NOT NULL DEFAULT CURRENT_TIME""".update.run,
    sql"""ALTER TABLE sql_entity ALTER COLUMN timestampTest TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP""".update.run,
    sql"""ALTER TABLE sql_entity ALTER COLUMN instantTest   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP""".update.run,
    sql"""ALTER TABLE sql_entity ALTER COLUMN localDateTest DATE      NOT NULL DEFAULT CURRENT_DATE""".update.run
  )
}
