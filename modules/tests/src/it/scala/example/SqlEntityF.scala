package example

import java.sql.{ Date, Time, Timestamp }
import java.time.{ Instant, LocalDate }

final case class SqlEntityF[NormalField[_], CreatedField[_]](
  shortTest:      CreatedField[Short],
  intTest:        CreatedField[Int],
  longTest:       CreatedField[Long],
  byteTest:       NormalField[Byte],
  floatTest:      NormalField[Float],
  doubleTest:     NormalField[Double],
  bigDecimalTest: NormalField[BigDecimal],
  stringTest:     NormalField[String],
  dateTest:       CreatedField[Date],
  timeTest:       CreatedField[Time],
  timestampTest:  CreatedField[Timestamp],
  instantTest:    CreatedField[Instant],
  localDateTest:  CreatedField[LocalDate]
)
