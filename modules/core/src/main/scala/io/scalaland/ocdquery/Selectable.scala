package io.scalaland.ocdquery

import io.scalaland.ocdquery.internal.Empty

sealed trait Selectable[+A] extends Product with Serializable {
  def toOption: Option[A] = this match {
    case Fixed(a) => Some(a)
    case Skipped  => None
  }
}

final case class Fixed[+A](to: A) extends Selectable[A]

case object Skipped extends Selectable[Nothing]

object Selectable {

  implicit def empty[A]: Empty[Selectable[A]] = () => Skipped
}
