package io.scalaland.ocdquery.internal

import shapeless._
import shapeless.ops.hlist.{ Length, Prepend, Split }

trait TupleAppender[A, B, C] {
  def append(a: A, b: B): C
  def revert(c: C): (A, B)
}

object TupleAppender extends TupleAppenderLowPriorityImplicit {

  def apply[A, B, C](implicit ta: TupleAppender[A, B, C]): TupleAppender[A, B, C] = ta

  implicit def appendTuple[A, ARep <: HList, ASize <: Nat, B, C <: Product, CGen <: HList](
    implicit aIsTuple: IsTuple[A],
    aGen:              Generic.Aux[A, ARep],
    aSize:             Length.Aux[ARep, ASize],
    prepend:           Prepend.Aux[ARep, B :: HNil, CGen],
    split:             Split.Aux[CGen, ASize, ARep, B :: HNil],
    cIsTuple:          IsTuple[C],
    cGen:              Generic.Aux[C, CGen]
  ): TupleAppender[A, B, C] = new TupleAppender[A, B, C] {

    aIsTuple.hashCode()
    aSize.hashCode()
    cIsTuple.hashCode()

    def append(a: A, b: B): C =
      cGen.from(prepend(aGen.to(a), b :: HNil))

    def revert(c: C): (A, B) = {
      val (aRep, b :: HNil) = split(cGen.to(c))
      aGen.from(aRep) -> b
    }
  }
}

trait TupleAppenderLowPriorityImplicit {

  implicit def appendNonTuple[A, B]: TupleAppender[A, B, (A, B)] = new TupleAppender[A, B, (A, B)] {

    def append(a: A, b: B): (A, B) = (a, b)

    def revert(c: (A, B)): (A, B) = c
  }
}
