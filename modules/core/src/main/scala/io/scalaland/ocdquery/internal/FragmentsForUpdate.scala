package io.scalaland.ocdquery.internal

import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.{ ColumnName, Updatable }
import shapeless._

import scala.annotation.implicitNotFound

@implicitNotFound(
  "Couldn't find/derive FragmentsForUpdate[${V}, ${C}]\n" +
    " - make sure that all fields are wrapped F[_], " +
    "and make sure doobie.Meta instances are available for all fields"
)
trait FragmentsForUpdate[V, C] {
  def toFragments(value: V, columns: C): List[(ColumnName[Any], Fragment)]
}

object FragmentsForUpdate {

  implicit val hnilFragmentsForUpdate: FragmentsForUpdate[HNil, HNil] = (_: HNil, _: HNil) => List.empty

  implicit def hconsFragmentsForUpdate[H, VT <: HList, CT <: HList](
    implicit meta: Meta[H],
    tail:          FragmentsForUpdate[VT, CT]
  ): FragmentsForUpdate[Updatable[H] :: VT, ColumnName[H] :: CT] =
    (v: Updatable[H] :: VT, c: ColumnName[H] :: CT) =>
      v.head.toOption match {
        case Some(value) => (c.head.as[Any] -> fr"$value") :: tail.toFragments(v.tail, c.tail)
        case None        => tail.toFragments(v.tail, c.tail)
    }

  implicit def productFragmentsForUpdate[V, C, VRep <: HList, CRep <: HList](
    implicit entryGen: Generic.Aux[V, VRep],
    columnsGen:        Generic.Aux[C, CRep],
    repCase:           FragmentsForUpdate[VRep, CRep]
  ): FragmentsForUpdate[V, C] =
    (value: V, columns: C) => repCase.toFragments(entryGen.to(value), columnsGen.to(columns))
}
