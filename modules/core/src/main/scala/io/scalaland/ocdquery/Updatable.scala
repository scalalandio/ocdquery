package io.scalaland.ocdquery

import io.scalaland.ocdquery.internal.Empty

sealed trait Updatable[+A] extends Product with Serializable {
  def toOption: Option[A] = this match {
    case UpdateTo(a) => Some(a)
    case Skip        => None
  }
}

final case class UpdateTo[+A](to: A) extends Updatable[A]

case object Skip extends Updatable[Nothing]

object Updatable {

  implicit def empty[A]: Empty[Updatable[A]] = () => Skip
}
