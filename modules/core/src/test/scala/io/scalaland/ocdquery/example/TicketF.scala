package io.scalaland.ocdquery.example

import java.time.LocalDate
import java.util.UUID

final case class TicketF[F[_], C[_]](
  id:      C[UUID],
  name:    F[String],
  surname: F[String],
  from:    F[String],
  to:      F[String],
  date:    F[LocalDate]
)
