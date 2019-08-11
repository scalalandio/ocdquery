package example

import java.sql.{ Date, Time, Timestamp }
import java.time.{ Instant, LocalDate }

final case class SqlValueF[Field[_]](
  shortTest:      Field[Short],
  intTest:        Field[Int],
  longTest:       Field[Long],
  byteTest:       Field[Byte],
  floatTest:      Field[Float],
  doubleTest:     Field[Double],
  bigDecimalTest: Field[BigDecimal],
  stringTest:     Field[String],
  dateTest:       Field[Date],
  timeTest:       Field[Time],
  timestampTest:  Field[Timestamp],
  instantTest:    Field[Instant],
  localDateTest:  Field[LocalDate]
)
