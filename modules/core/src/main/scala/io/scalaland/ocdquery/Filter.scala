package io.scalaland.ocdquery

import doobie._
import doobie.implicits._

trait Filter { filter0 =>
  def fragment(): Fragment

  def and(filter: Filter): Filter = () => fr"(" ++ filter0.fragment ++ fr"AND" ++ filter.fragment ++ fr")"
  def or(filter:  Filter): Filter = () => fr"(" ++ filter0.fragment ++ fr"OR" ++ filter.fragment ++ fr")"
  def not: Filter = () => fr"NOT (" ++ filter0.fragment ++ fr")"
}
