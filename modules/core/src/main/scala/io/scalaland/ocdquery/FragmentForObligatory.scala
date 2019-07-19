package io.scalaland.ocdquery

import cats.implicits._
import doobie._
import doobie.implicits._
import shapeless._

trait FragmentForObligatory[V, C] {
  def toFragment(value: V, columns: C): Option[Fragment]
}

object FragmentForObligatory extends LowPriorityFragmentForObligatoryImplicit {

  implicit val hnilCase: FragmentForObligatory[HNil, HNil] = (_: HNil, _: HNil) => None

  // skips selectable element
  implicit def hconsSelectableCase[H, VT <: HList, CT <: HList](
    implicit tail: FragmentForObligatory[VT, CT]
  ): FragmentForObligatory[Selectable[H] :: VT, String :: CT] =
    (v: Selectable[H] :: VT, c: String :: CT) => tail.toFragment(v.tail, c.tail)

  implicit def productCase[V, C, VRep <: HList, CRep <: HList](
    implicit entryGen: Generic.Aux[V, VRep],
    columnsGen:        Generic.Aux[C, CRep],
    repCase:           FragmentForObligatory[VRep, CRep]
  ): FragmentForObligatory[V, C] =
    (value: V, columns: C) => repCase.toFragment(entryGen.to(value), columnsGen.to(columns))
}

trait LowPriorityFragmentForObligatoryImplicit {

  // takes non-selectable (obligatory) elements
  implicit def hconsObligatoryCase[H, VT <: HList, CT <: HList](
    implicit meta: Meta[H],
    tail:          FragmentForObligatory[VT, CT]
  ): FragmentForObligatory[H :: VT, String :: CT] =
    (v: H :: VT, c: String :: CT) =>
      Option(Fragment.const(s"${c.head} = ") ++ fr"${v.head}") |+| tail.toFragment(v.tail, c.tail)
}
