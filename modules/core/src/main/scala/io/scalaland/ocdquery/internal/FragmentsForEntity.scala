package io.scalaland.ocdquery.internal

import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.ColumnName
import shapeless._

import scala.annotation.implicitNotFound

@implicitNotFound(
  "Couldn't find/derive FragmentsForEntity[${V}, ${C}]\n" +
    " - make sure that all fields are wrapped in F[_], " +
    "and make sure doobie.Meta instances are available for all fields"
)
trait FragmentsForEntity[V, C] {
  def toFragments(value: V, columns: C): List[(ColumnName, Fragment)]
}

object FragmentsForEntity {

  implicit val hnilCase: FragmentsForEntity[HNil, HNil] = (_: HNil, _: HNil) => List.empty

  implicit def hconsCase[H, VT <: HList, CT <: HList](
    implicit meta: Meta[H],
    tail:          FragmentsForEntity[VT, CT]
  ): FragmentsForEntity[H :: VT, String :: CT] =
    (v: H :: VT, c: String :: CT) => (c.head -> fr"${v.head}") :: tail.toFragments(v.tail, c.tail)

  implicit def productCase[V, C, VRep <: HList, CRep <: HList](
    implicit entryGen: Generic.Aux[V, VRep],
    columnsGen:        Generic.Aux[C, CRep],
    repCase:           FragmentsForEntity[VRep, CRep]
  ): FragmentsForEntity[V, C] =
    (value: V, columns: C) => repCase.toFragments(entryGen.to(value), columnsGen.to(columns))
}
