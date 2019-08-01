package io.scalaland.ocdquery

import shapeless._
import shapeless.labelled.FieldType

trait DefaultColumnNames[A] {

  def get(): A
}

object DefaultColumnNames {

  def forValue[ValueF[_[_]]](implicit default: DefaultColumnNames[ValueF[ColumnName]]): ValueF[ColumnName] =
    default.get()

  def forEntity[EntityF[_[_], _[_]]](
    implicit default: DefaultColumnNames[EntityF[ColumnName, ColumnName]]
  ): EntityF[ColumnName, ColumnName] = default.get()

  implicit val hnilCase: DefaultColumnNames[HNil] = () => HNil

  implicit def hconsCase[H, LH <: Symbol, T <: HList](
    implicit label: Witness.Aux[LH],
    tailDefault:    DefaultColumnNames[T]
  ): DefaultColumnNames[FieldType[LH, ColumnName[H]] :: T] =
    () => labelled.field[LH][ColumnName[H]](label.value.name.columnName[H]) :: tailDefault.get()

  implicit def productCase[P, Rep <: HList](implicit pGen: LabelledGeneric.Aux[P, Rep],
                                            defaultRep:    DefaultColumnNames[Rep]): DefaultColumnNames[P] =
    () => pGen.from(defaultRep.get())
}
