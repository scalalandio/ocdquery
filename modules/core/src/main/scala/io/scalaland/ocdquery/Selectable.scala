package io.scalaland.ocdquery

sealed trait Selectable[+A] extends Product with Serializable {
  def toOption: Option[A] = this match {
    case Fixed(a) => Some(a)
    case Skipped  => None
  }
}

final case class Fixed[+A](to: A) extends Selectable[A]

case object Skipped extends Selectable[Nothing]
