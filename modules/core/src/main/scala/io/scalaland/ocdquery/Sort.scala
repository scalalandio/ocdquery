package io.scalaland.ocdquery

sealed trait Sort extends Product with Serializable
object Sort {
  case object Ascending extends Sort
  case object Descending extends Sort
}
