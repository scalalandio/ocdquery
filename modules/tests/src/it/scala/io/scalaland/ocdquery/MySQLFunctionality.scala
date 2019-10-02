package io.scalaland.ocdquery

import cats.effect.{ Blocker, IO, Resource }
import doobie._
import doobie.implicits._
import doobie.hikari.HikariTransactor
import org.specs2.mutable.Specification

final class MySQLFunctionality extends Specification with TestCommonFeatures {

  override val isMySql = true

  protected def makeTransactor: Resource[IO, Transactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32) // scalastyle:ignore
      te <- ExecutionContexts.cachedThreadPool[IO]
      xa <- HikariTransactor.newHikariTransactor[IO](
        driverClassName = classOf[com.mysql.cj.jdbc.Driver].getName,
        url             = "jdbc:mysql://localhost:3306/ocdquery",
        user            = "ocdquery",
        pass            = "password",
        connectEC       = ce,
        blocker         = Blocker.liftExecutionContext(te)
      )
    } yield xa

  override protected val initSql: Seq[ConnectionIO[Int]] = super.initSql ++ Seq(
    sql"""ALTER TABLE sql_entity MODIFY COLUMN shortTest INT auto_increment PRIMARY KEY""".update.run,
    sql"""CREATE TRIGGER sql_entity_defaults BEFORE INSERT ON sql_entity FOR EACH ROW
          BEGIN
          SET NEW.intTest       = NEW.shortTest,
              NEW.longTest      = NEW.shortTest,
              NEW.dateTest      = NOW(),
              NEW.timeTest      = NOW(),
              NEW.timestampTest = NOW(),
              NEW.instantTest   = NOW(),
              NEW.localDateTest = NOW();
          END""".update.run
  )
}
