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
  def getList(c: C): List[ColumnName[Any]]
}

object AllColumns {

  implicit val hnilAllColumns: AllColumns[HNil] = _ => List.empty

  implicit def hconsAllColumns[A, C <: HList](implicit cColumns: AllColumns[C]): AllColumns[ColumnName[A] :: C] =
    (sc: ColumnName[A] :: C) => sc.head.as[Any] :: cColumns.getList(sc.tail)

  implicit def productAllColumns[A, B <: HList](implicit rep: Generic.Aux[A, B],
                                                hlistColumns: AllColumns[B]): AllColumns[A] =
    (a: A) => hlistColumns.getList(rep.to(a))
}
