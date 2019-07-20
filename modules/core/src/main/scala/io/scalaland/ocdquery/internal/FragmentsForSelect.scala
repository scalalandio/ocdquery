package io.scalaland.ocdquery.internal

import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.{ ColumnName, Selectable }
import shapeless._

import scala.annotation.implicitNotFound

@implicitNotFound(
  "Couldn't find/derive FragmentsForSelect[${V}, ${C}]\n" +
    " - make sure that all fields are wrapped F[_], " +
    "and make sure doobie.Meta instances are available for all fields"
)
trait FragmentsForSelect[V, C] {
  def toFragments(value: V, columns: C): List[(ColumnName, Fragment)]
}

object FragmentsForSelect {

  implicit val hnilCase: FragmentsForSelect[HNil, HNil] = (_: HNil, _: HNil) => List.empty

  implicit def hconsCase[H, VT <: HList, CT <: HList](
    implicit meta: Meta[H],
    tail:          FragmentsForSelect[VT, CT]
  ): FragmentsForSelect[Selectable[H] :: VT, String :: CT] =
    (v: Selectable[H] :: VT, c: String :: CT) =>
      v.head.toOption match {
        case Some(value) => (c.head -> fr"$value") :: tail.toFragments(v.tail, c.tail)
        case None        => tail.toFragments(v.tail, c.tail)
    }

  implicit def productCase[V, C, VRep <: HList, CRep <: HList](
    implicit entryGen: Generic.Aux[V, VRep],
    columnsGen:        Generic.Aux[C, CRep],
    repCase:           FragmentsForSelect[VRep, CRep]
  ): FragmentsForSelect[V, C] =
    (value: V, columns: C) => repCase.toFragments(entryGen.to(value), columnsGen.to(columns))
}
