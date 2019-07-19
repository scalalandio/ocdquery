package io.scalaland.ocdquery

import doobie._
import doobie.implicits._
import shapeless._

trait FragmentForSelectable[V, C] {
  def toFragment(value: V, columns: C): List[(ColumnName, Fragment)]
}

object FragmentForSelectable extends LowPriorityFragmentForUpdatableImplicit {

  implicit val hnilCase: FragmentForSelectable[HNil, HNil] = (_: HNil, _: HNil) => List.empty

  // gets selected elements
  implicit def hconsSelectableCase[H, VT <: HList, CT <: HList](
    implicit meta: Meta[H],
    tail:          FragmentForSelectable[VT, CT]
  ): FragmentForSelectable[Selectable[H] :: VT, String :: CT] =
    (v: Selectable[H] :: VT, c: String :: CT) =>
      v.head.toOption match {
        case Some(value) => (c.head -> fr"$value") :: tail.toFragment(v.tail, c.tail)
        case None        => tail.toFragment(v.tail, c.tail)
    }

  implicit def productCase[V, C, VRep <: HList, CRep <: HList](
    implicit entryGen: Generic.Aux[V, VRep],
    columnsGen:        Generic.Aux[C, CRep],
    repCase:           FragmentForSelectable[VRep, CRep]
  ): FragmentForSelectable[V, C] =
    (value: V, columns: C) => repCase.toFragment(entryGen.to(value), columnsGen.to(columns))
}

trait LowPriorityFragmentForUpdatableImplicit {

  // skips non-selectable element
  implicit def hconsObligatoryCase[H, VT <: HList, CT <: HList](
    implicit tail: FragmentForSelectable[VT, CT]
  ): FragmentForSelectable[H :: VT, String :: CT] =
    (v: H :: VT, c: String :: CT) => tail.toFragment(v.tail, c.tail)
}
