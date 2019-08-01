package io.scalaland.ocdquery.example

import com.softwaremill.quicklens._
import doobie.h2.implicits._
import io.scalaland.ocdquery._

object TicketRepo
    extends Repo(RepoMeta.forEntity("tickets", DefaultColumnNames.forEntity[TicketF].modify(_.from).setTo("from_")))
