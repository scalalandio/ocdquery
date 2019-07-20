package io.scalaland.ocdquery.internal

import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.ColumnName
import shapeless._

import scala.annotation.implicitNotFound

@implicitNotFound(
  "Couldn't find/derive FragmentsForCreate[${V}, ${C}]\n" +
    " - make sure that all fields are wrapped F[_], (with created fields also wrapped in G[_] = G[F[A]])" +
    "and make sure doobie.Meta instances are available for all fields"
)
trait FragmentsForCreate[V, C] {
  def toFragments(value: V, columns: C): List[(ColumnName, Fragment)]
}

object FragmentsForCreate extends LowPriorityFragmentsForCreate {

  implicit val hnilCase: FragmentsForCreate[HNil, HNil] = (_: HNil, _: HNil) => List.empty

  // skip unit types
  implicit def hconsUnitCase[VT <: HList, CT <: HList](
    implicit tail: FragmentsForCreate[VT, CT]
  ): FragmentsForCreate[Unit :: VT, String :: CT] =
    (v: Unit :: VT, c: String :: CT) => tail.toFragments(v.tail, c.tail)

  implicit def productCase[V, C, VRep <: HList, CRep <: HList](
    implicit entryGen: Generic.Aux[V, VRep],
    columnsGen:        Generic.Aux[C, CRep],
    repCase:           FragmentsForCreate[VRep, CRep]
  ): FragmentsForCreate[V, C] =
    (value: V, columns: C) => repCase.toFragments(entryGen.to(value), columnsGen.to(columns))
}

trait LowPriorityFragmentsForCreate {

  implicit def hconsCase[H, VT <: HList, CT <: HList](
    implicit meta: Meta[H],
    tail:          FragmentsForCreate[VT, CT]
  ): FragmentsForCreate[H :: VT, String :: CT] =
    (v: H :: VT, c: String :: CT) => (c.head -> fr"${v.head}") :: tail.toFragments(v.tail, c.tail)
}
