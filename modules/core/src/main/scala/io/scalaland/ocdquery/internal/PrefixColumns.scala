package io.scalaland.ocdquery.internal

import io.scalaland.ocdquery.ColumnName
import shapeless._

trait PrefixColumns[C] {

  def prepend(columns: C, prefix: String): C
}

object PrefixColumns {

  @inline def apply[C](implicit p: PrefixColumns[C]): PrefixColumns[C] = p

  implicit val hnilPrefixColumns: PrefixColumns[HNil] = (cols, _) => cols

  implicit def hconsPrefixColumns[H, T <: HList](
    implicit tPrefix: PrefixColumns[T]
  ): PrefixColumns[ColumnName[H] :: T] =
    (cols, prefix) => ColumnName[H](prefix + "." + cols.head.name) :: tPrefix.prepend(cols.tail, prefix)

  implicit def productPrefixColumns[P, PRep <: HList](implicit pGen: Generic.Aux[P, PRep],
                                                      pPrefix:       PrefixColumns[PRep]): PrefixColumns[P] =
    (cols, prefix) => pGen.from(pPrefix.prepend(pGen.to(cols), prefix))
}
