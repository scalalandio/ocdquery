package io.scalaland.ocdquery.internal

import doobie._
import doobie.implicits._
import io.scalaland.ocdquery.{ Skip, Updatable, UpdateTo }
import magnolia.{ CaseClass, Magnolia, SealedTrait }
import scala.language.experimental.macros

trait FragmentByField[T] {
  def apply(t: T): List[(String, Fragment)]
}

object FragmentByField extends FragmentByFieldLowLevelImplicit {

  type Typeclass[T] = FragmentByField[T]

  def combine[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] =
    t =>
      caseClass.parameters.toList.flatMap { param =>
        param.typeclass(param.dereference(t)).map {
          case (label: String, columnName) =>
            if (label.isEmpty) param.label -> columnName else label -> columnName
        }
    }

  def dispatch[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = ???

  implicit def gen[T]: Typeclass[T] = macro Magnolia.gen[T]

  implicit def forUnit: FragmentByField[Unit] = _ => List.empty

  implicit def forUpdatable[A: Meta]: FragmentByField[Updatable[A]] = {
    case UpdateTo(value) => List("" -> fr"$value")
    case Skip            => List.empty
  }
}

trait FragmentByFieldLowLevelImplicit { self: FragmentByField.type =>

  implicit def forMeta[A: Meta]: FragmentByField[A] = a => List("" -> fr"$a")
}