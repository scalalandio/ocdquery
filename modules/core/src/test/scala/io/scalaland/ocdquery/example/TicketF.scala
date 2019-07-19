package io.scalaland.ocdquery.example

import java.time.LocalDate
import java.util.UUID

final case class TicketF[C[_], U[_]](
  id:      C[UUID],
  name:    U[String],
  surname: U[String],
  from:    U[String],
  to:      U[String],
  date:    U[LocalDate]
)
