package io.scalaland.ocdquery

import doobie.util.fragment.Fragment

final case class ColumnName[A](val name: String) extends AnyVal {

  @inline def as[B]: ColumnName[B] = ColumnName[B](name)

  @inline def fragment: Fragment = Fragment.const(name)
}
