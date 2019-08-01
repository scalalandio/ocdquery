package io.scalaland.ocdquery

final case class ColumnName[A](val name: String) extends AnyVal {

  @inline def as[B]: ColumnName[B] = ColumnName[B](name)
}
