package io.scalaland.ocdquery.internal

import io.scalaland.ocdquery.missingshapeless.Untupler
import shapeless._

/*
 * Used for figuring out the tuple version of a Create object but without Unit field,
 * and then transforming that tuple into Create.
 */
trait SkipUnit[Create] {
  type SU
  def from(skipped: SU): Create
}

object SkipUnit {

  type Aux[C, SU0] = SkipUnit[C] { type SU = SU0 }

  implicit val hnilCase: Aux[HNil, HNil] = new SkipUnit[HNil] {
    type SU = HNil
    def from(skipped: HNil): HNil = skipped
  }

  implicit def hconsUnitCase[SU0 <: HList, C <: HList](implicit skip: Aux[C, SU0]): Aux[Unit :: C, SU0] =
    new SkipUnit[Unit :: C] {
      type SU = SU0
      def from(skipped: SU0): Unit :: C = () :: skip.from(skipped)
    }

  implicit def hconsNonUnitCase[H, SU0 <: HList, C <: HList](implicit skip: Aux[C, SU0],
                                                             ev:            H =:!= Unit): Aux[H :: C, H :: SU0] =
    new SkipUnit[H :: C] {
      ev.hashCode() // suppress unused
      type SU = H :: SU0
      def from(skipped: SU): H :: C = skipped.head :: skip.from(skipped.tail)
    }

  implicit def productCase[C, CRep <: HList, SURep <: HList](implicit
                                                             cGen:     Generic.Aux[C, CRep],
                                                             skip:     Aux[CRep, SURep],
                                                             untupler: Untupler[SURep]): Aux[C, untupler.In] =
    new SkipUnit[C] {
      type SU = untupler.In
      def from(skipped: untupler.In): C = cGen.from(skip.from(untupler(skipped)))
    }
}
