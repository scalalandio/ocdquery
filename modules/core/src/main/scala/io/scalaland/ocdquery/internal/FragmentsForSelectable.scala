package io.scalaland.ocdquery.internal

import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.{ ColumnName, Selectable }
import shapeless._

import scala.annotation.implicitNotFound

@implicitNotFound(
  "Couldn't find/derive FragmentsForSelectable[$C, $V]\n" +
    " - make sure that all fields are wrapped in obligatory or selectable F[_], " +
    "and make sure doobie.Meta instances are available for all fields"
)
trait FragmentsForSelectable[V, C] {
  def toFragments(value: V, columns: C): List[(ColumnName, Fragment)]
}

object FragmentsForSelectable extends LowPriorityFragmentForUpdatableImplicit {

  implicit val hnilCase: FragmentsForSelectable[HNil, HNil] = (_: HNil, _: HNil) => List.empty

  // gets selected elements
  implicit def hconsSelectableCase[H, VT <: HList, CT <: HList](
    implicit meta: Meta[H],
    tail:          FragmentsForSelectable[VT, CT]
  ): FragmentsForSelectable[Selectable[H] :: VT, String :: CT] =
    (v: Selectable[H] :: VT, c: String :: CT) =>
      v.head.toOption match {
        case Some(value) => (c.head -> fr"$value") :: tail.toFragments(v.tail, c.tail)
        case None        => tail.toFragments(v.tail, c.tail)
    }

  implicit def productCase[V, C, VRep <: HList, CRep <: HList](
    implicit entryGen: Generic.Aux[V, VRep],
    columnsGen:        Generic.Aux[C, CRep],
    repCase:           FragmentsForSelectable[VRep, CRep]
  ): FragmentsForSelectable[V, C] =
    (value: V, columns: C) => repCase.toFragments(entryGen.to(value), columnsGen.to(columns))
}

trait LowPriorityFragmentForUpdatableImplicit {

  // skips non-selectable element
  implicit def hconsObligatoryCase[H, VT <: HList, CT <: HList](
    implicit tail: FragmentsForSelectable[VT, CT]
  ): FragmentsForSelectable[H :: VT, String :: CT] =
    (v: H :: VT, c: String :: CT) => tail.toFragments(v.tail, c.tail)
}
