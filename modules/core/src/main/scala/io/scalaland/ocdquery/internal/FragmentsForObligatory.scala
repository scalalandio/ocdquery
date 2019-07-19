package io.scalaland.ocdquery.internal

import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.{ ColumnName, Selectable }
import shapeless._

import scala.annotation.implicitNotFound

@implicitNotFound(
  "Couldn't find/derive FragmentsForObligatory[$C, $V]\n" +
    " - make sure that all fields are wrapped in obligatory or selectable F[_], " +
    "and make sure doobie.Meta instances are available for all fields"
)
trait FragmentsForObligatory[V, C] {
  def toFragments(value: V, columns: C): List[(ColumnName, Fragment)]
}

object FragmentsForObligatory extends LowPriorityFragmentForObligatoryImplicit {

  implicit val hnilCase: FragmentsForObligatory[HNil, HNil] = (_: HNil, _: HNil) => List.empty

  // skips selectable element
  implicit def hconsSelectableCase[H, VT <: HList, CT <: HList](
    implicit tail: FragmentsForObligatory[VT, CT]
  ): FragmentsForObligatory[Selectable[H] :: VT, String :: CT] =
    (v: Selectable[H] :: VT, c: String :: CT) => tail.toFragments(v.tail, c.tail)

  implicit def productCase[V, C, VRep <: HList, CRep <: HList](
    implicit entryGen: Generic.Aux[V, VRep],
    columnsGen:        Generic.Aux[C, CRep],
    repCase:           FragmentsForObligatory[VRep, CRep]
  ): FragmentsForObligatory[V, C] =
    (value: V, columns: C) => repCase.toFragments(entryGen.to(value), columnsGen.to(columns))
}

trait LowPriorityFragmentForObligatoryImplicit {

  // takes non-selectable (obligatory) elements
  implicit def hconsObligatoryCase[H, VT <: HList, CT <: HList](
    implicit meta: Meta[H],
    tail:          FragmentsForObligatory[VT, CT]
  ): FragmentsForObligatory[H :: VT, String :: CT] =
    (v: H :: VT, c: String :: CT) => (c.head -> fr"${v.head}") :: tail.toFragments(v.tail, c.tail)
}
