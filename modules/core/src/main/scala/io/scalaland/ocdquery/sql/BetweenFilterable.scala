package io.scalaland.ocdquery.sql

import java.sql.{ Date, Time, Timestamp }
import java.time.{ Instant, LocalDate }

import doobie.{ Fragment, Put }
import doobie.implicits._
import shapeless._

/*
 * Type can be safely put into BETWEEN condition
 */
trait BetweenFilterable[A] {

  def between(begin: A, end: A): Fragment

  def contramap[B](f: B => A): BetweenFilterable[B] = (begin, end) => between(f(begin), f(end))
}
object BetweenFilterable {

  def instant[A: Put]: BetweenFilterable[A] = (begin, end) => fr"BETWEEN $begin AND $end"

  implicit val betweenFilterableString: BetweenFilterable[String] = instant[String]

  implicit val betweenFilterableByte:       BetweenFilterable[Byte]       = instant[Byte]
  implicit val betweenFilterableShort:      BetweenFilterable[Short]      = instant[Short]
  implicit val betweenFilterableInt:        BetweenFilterable[Int]        = instant[Int]
  implicit val betweenFilterableLong:       BetweenFilterable[Long]       = instant[Long]
  implicit val betweenFilterableFloat:      BetweenFilterable[Float]      = instant[Float]
  implicit val betweenFilterableDouble:     BetweenFilterable[Double]     = instant[Double]
  implicit val betweenFilterableBigDecimal: BetweenFilterable[BigDecimal] = instant[BigDecimal]

  implicit val betweenFilterableDate:      BetweenFilterable[Date]      = instant[Date]
  implicit val betweenFilterableTime:      BetweenFilterable[Time]      = instant[Time]
  implicit val betweenFilterableTimestamp: BetweenFilterable[Timestamp] = instant[Timestamp]
  implicit val betweenFilterableInstant:   BetweenFilterable[Instant]   = instant[Instant]
  implicit val betweenFilterableLocalDate: BetweenFilterable[LocalDate] = instant[LocalDate]

  // TODO: check if this works
  implicit def betweenFilterableAnyVal[A, B: BetweenFilterable](
    implicit gen: Generic.Aux[A, B :: HNil]
  ): BetweenFilterable[A] =
    implicitly[BetweenFilterable[B]].contramap[A](a => gen.to(a).head)
}
