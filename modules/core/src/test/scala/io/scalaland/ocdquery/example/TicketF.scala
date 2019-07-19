package io.scalaland.ocdquery.example

import java.time.LocalDate
import java.util.UUID

final case class TicketF[O[_], S[_]](
  id:      O[UUID],
  name:    S[String],
  surname: S[String],
  from:    S[String],
  to:      S[String],
  date:    S[LocalDate]
)
