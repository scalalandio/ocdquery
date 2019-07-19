package io.scalaland.ocdquery.internal

import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.ColumnName
import shapeless._

import scala.annotation.implicitNotFound

@implicitNotFound(
  "Couldn't find/derive FragmentsForAll[$C, $V]\n" +
    " - make sure that all fields are wrapped in obligatory or selectable F[_], " +
    "and make sure doobie.Meta instances are available for all fields"
)
trait FragmentsForAll[V, C] {
  def toFragments(value: V, columns: C): List[(ColumnName, Fragment)]
}

object FragmentsForAll {

  implicit val hnilCase: FragmentsForAll[HNil, HNil] = (_: HNil, _: HNil) => List.empty

  implicit def hconsCase[H, VT <: HList, CT <: HList](
    implicit meta: Meta[H],
    tail:          FragmentsForAll[VT, CT]
  ): FragmentsForAll[H :: VT, String :: CT] =
    (v: H :: VT, c: String :: CT) => (c.head -> fr"${v.head}") :: tail.toFragments(v.tail, c.tail)

  implicit def productCase[V, C, VRep <: HList, CRep <: HList](
    implicit entryGen: Generic.Aux[V, VRep],
    columnsGen:        Generic.Aux[C, CRep],
    repCase:           FragmentsForAll[VRep, CRep]
  ): FragmentsForAll[V, C] =
    (value: V, columns: C) => repCase.toFragments(entryGen.to(value), columnsGen.to(columns))
}
