package io.scalaland.ocdquery

import doobie._
import shapeless._

trait RepoMeta[EntryF[_[_], _[_]]] {
  type Entry = EntryF[Id, Id]
  type Fixed = EntryF[Id, Selectable]

  val tableName:             String
  val fragmentForAll:        Entry => Fragment
  val fragmentForSelectable: Fixed => Option[Fragment]
  val fragmentForObligatory: Fixed => Option[Fragment]
}

object RepoMeta {
  type ColumnName[_] = String

  def instanceFor[EntryF[_[_], _[_]]](
    table:   String,
    columns: EntryF[RepoMeta.ColumnName, RepoMeta.ColumnName]
  )(
    implicit forAll: FragmentForAll[EntryF[Id, Id], EntryF[RepoMeta.ColumnName, RepoMeta.ColumnName]],
    forSelectable:   FragmentForSelectable[EntryF[Id, Selectable], EntryF[RepoMeta.ColumnName, RepoMeta.ColumnName]],
    forObligatory:   FragmentForObligatory[EntryF[Id, Selectable], EntryF[RepoMeta.ColumnName, RepoMeta.ColumnName]]
  ): RepoMeta[EntryF] =
    new RepoMeta[EntryF] {
      val tableName:             String                    = table
      val fragmentForAll:        Entry => Fragment         = forAll.toFragment(_, columns)
      val fragmentForSelectable: Fixed => Option[Fragment] = forSelectable.toFragment(_, columns)
      val fragmentForObligatory: Fixed => Option[Fragment] = forObligatory.toFragment(_, columns)
    }
}
