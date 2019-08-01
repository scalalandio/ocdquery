package io.scalaland.ocdquery.missingshapeless

import shapeless._
import shapeless.ops.hlist.{ Length, Prepend, Split, Tupler }

trait TupleAppender[A, B] {
  type Out
  def append(a:   A, b: B): Out
  def revert(out: Out): (A, B)
}

object TupleAppender extends TupleAppenderLowPriorityImplicit {

  @inline def apply[A, B](implicit ta: TupleAppender[A, B]): TupleAppender[A, B] = ta

  type Aux[A, B, C] = TupleAppender[A, B] { type Out = C }

  // TODO: consider unrolling

  implicit def appendTuple[A, ARep <: HList, ASize <: Nat, B, CGen <: HList, C](
    implicit aTupler: Tupler.Aux[ARep, A],
    aUntupler:        Untupler.Aux[ARep, A],
    aSize:            Length.Aux[ARep, ASize],
    prepend:          Prepend.Aux[ARep, B :: HNil, CGen],
    split:            Split.Aux[CGen, ASize, ARep, B :: HNil],
    cTupler:          Tupler.Aux[CGen, C],
    cUntupler:        Untupler.Aux[CGen, C]
  ): TupleAppender.Aux[A, B, C] = new TupleAppender[A, B] {
    type Out = C
    aSize.hashCode()
    def append(a: A, b: B): Out =
      cTupler(prepend(aUntupler(a), b :: HNil))
    def revert(c: Out): (A, B) =
      split(cUntupler(c)) match { case (aRep, b :: HNil) => aTupler(aRep) -> b }
  }
}

trait TupleAppenderLowPriorityImplicit {

  implicit def appendNonTuple[A, B](
    implicit ev: Refute[IsTuple[A]]
  ): TupleAppender.Aux[A, B, (A, B)] = new TupleAppender[A, B] {
    type Out = (A, B)
    ev.hashCode()
    def append(a: A, b: B): Out = (a, b)
    def revert(c: Out): (A, B) = c
  }
}
