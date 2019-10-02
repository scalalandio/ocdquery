package io.scalaland.ocdquery

import cats.effect._
import doobie.h2.H2Transactor
import doobie.specs2.analysisspec.IOChecker
import doobie.util.transactor.Transactor
import doobie.util.ExecutionContexts
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll

import scala.concurrent.ExecutionContext

trait WithH2Database extends BeforeAfterAll with IOChecker { this: Specification =>

  private implicit val cs = IO.contextShift(ExecutionContext.global)

  private val (xa, close) =
    (for {
      ce <- ExecutionContexts.fixedThreadPool[IO](32) // scalastyle:ignore
      te <- ExecutionContexts.cachedThreadPool[IO] // our transaction EC
      xa <- H2Transactor.newH2Transactor[IO](
        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", // connect URL
        "sa", // username
        "", // password
        ce, // await connection here
        Blocker.liftExecutionContext(te) // execute JDBC operations here
      )
    } yield xa).allocated.unsafeRunSync()

  override def transactor: Transactor[IO] = xa

  override def afterAll(): Unit =
    close.unsafeRunSync()
}
