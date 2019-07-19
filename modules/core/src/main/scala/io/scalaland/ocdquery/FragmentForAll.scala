package io.scalaland.ocdquery

import doobie._
import doobie.implicits._
import shapeless._

trait FragmentForAll[V, C] {
  def toFragment(value: V, columns: C): List[(ColumnName, Fragment)]
}

object FragmentForAll {

  implicit val hnilCase: FragmentForAll[HNil, HNil] = (_: HNil, _: HNil) => List.empty

  implicit def hconsCase[H, VT <: HList, CT <: HList](
    implicit meta: Meta[H],
    tail:          FragmentForAll[VT, CT]
  ): FragmentForAll[H :: VT, String :: CT] =
    (v: H :: VT, c: String :: CT) => (c.head -> fr"${v.head}") :: tail.toFragment(v.tail, c.tail)

  implicit def productCase[V, C, VRep <: HList, CRep <: HList](
    implicit entryGen: Generic.Aux[V, VRep],
    columnsGen:        Generic.Aux[C, CRep],
    repCase:           FragmentForAll[VRep, CRep]
  ): FragmentForAll[V, C] =
    (value: V, columns: C) => repCase.toFragment(entryGen.to(value), columnsGen.to(columns))
}
