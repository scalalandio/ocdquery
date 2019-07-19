package io.scalaland

import cats.Id

package object ocdquery {

  type ColumnName     = String
  type ColumnNameF[_] = String

  type EntityOf[EntityF[_[_], _[_]]]  = EntityF[Id, Id]
  type SelectOf[EntityF[_[_], _[_]]]  = EntityF[Id, Selectable]
  type ColumnsOf[EntityF[_[_], _[_]]] = EntityF[ColumnNameF, ColumnNameF]
}
