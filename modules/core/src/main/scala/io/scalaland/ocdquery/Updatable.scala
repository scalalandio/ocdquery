package io.scalaland.ocdquery

/*
 * Virtually Option, but as a new type, to avoid confusion around
 * Option[Option[A]] if you were trying to update optional field.
 */
sealed trait Updatable[+A] extends Product with Serializable {
  def toOption: Option[A] = this match {
    case UpdateTo(a) => Some(a)
    case Skip        => None
  }
}

final case class UpdateTo[+A](to: A) extends Updatable[A]

case object Skip extends Updatable[Nothing]
