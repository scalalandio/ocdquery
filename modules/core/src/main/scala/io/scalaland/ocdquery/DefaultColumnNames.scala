package io.scalaland.ocdquery

import shapeless._
import shapeless.labelled.FieldType

trait DefaultColumnNames[A] {

  def get(): A
}

object DefaultColumnNames {

  def forValue[ValueF[_[_]]](implicit default: DefaultColumnNames[ValueF[ColumnNameF]]): ValueF[ColumnNameF] =
    default.get()

  def forEntity[EntityF[_[_], _[_]]](
    implicit default: DefaultColumnNames[EntityF[ColumnNameF, ColumnNameF]]
  ): EntityF[ColumnNameF, ColumnNameF] = default.get()

  implicit val hnilCase: DefaultColumnNames[HNil] = () => HNil

  implicit def hconsCase[LH <: Symbol, T <: HList](
    implicit label: Witness.Aux[LH],
    tailDefault:    DefaultColumnNames[T]
  ): DefaultColumnNames[FieldType[LH, ColumnName] :: T] =
    () => labelled.field[LH][ColumnName](label.value.name) :: tailDefault.get()

  implicit def productCase[P, Rep <: HList](implicit pGen: LabelledGeneric.Aux[P, Rep],
                                            defaultRep:    DefaultColumnNames[Rep]): DefaultColumnNames[P] =
    () => pGen.from(defaultRep.get())
}
