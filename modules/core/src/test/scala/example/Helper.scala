package example

import cats.Id
import doobie._
//import doobie.implicits._
import doobie.h2.implicits._

object Helper {
  UuidType.hashCode()

  val read = implicitly[Read[TicketF[Id, Id]]]
}
