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
  def toFragments(value: V, columns: C): List[(ColumnName[Any], Fragment)]
}

object FragmentsForEntity {

  implicit val hnilFragmentsForEntity: FragmentsForEntity[HNil, HNil] = (_: HNil, _: HNil) => List.empty

  implicit def hconsFragmentsForEntity[H, VT <: HList, CT <: HList](
    implicit meta: Meta[H],
    tail:          FragmentsForEntity[VT, CT]
  ): FragmentsForEntity[H :: VT, ColumnName[H] :: CT] =
    (v: H :: VT, c: ColumnName[H] :: CT) => (c.head.as[Any] -> fr"${v.head}") :: tail.toFragments(v.tail, c.tail)

  implicit def productFragmentsForEntity[V, C, VRep <: HList, CRep <: HList](
    implicit entryGen: Generic.Aux[V, VRep],
    columnsGen:        Generic.Aux[C, CRep],
    repCase:           FragmentsForEntity[VRep, CRep]
  ): FragmentsForEntity[V, C] =
    (value: V, columns: C) => repCase.toFragments(entryGen.to(value), columnsGen.to(columns))
}
