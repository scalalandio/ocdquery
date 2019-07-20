package io.scalaland.ocdquery.internal

import io.scalaland.ocdquery.ColumnName
import shapeless._

import scala.annotation.implicitNotFound

@implicitNotFound(
  "Couldn't find/derive AllColumns[${C}]\n" +
    " - make sure that all fields are wrapped in obligatory or selectable F[_], " +
    "so that ${C} is made of Strings only"
)
trait AllColumns[C] {
  def getList(c: C): List[ColumnName]
}

object AllColumns {

  implicit val hnilCase: AllColumns[HNil] = _ => List.empty

  implicit def hconsCase[C <: HList](implicit cColumns: AllColumns[C]): AllColumns[ColumnName :: C] =
    (sc: ColumnName :: C) => sc.head :: cColumns.getList(sc.tail)

  implicit def productCase[A, B <: HList](implicit rep: Generic.Aux[A, B], hlistColumns: AllColumns[B]): AllColumns[A] =
    (a: A) => hlistColumns.getList(rep.to(a))
}
