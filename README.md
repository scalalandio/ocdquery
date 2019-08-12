# Over-Complicated Database Query (OCD Query)

[![Build Status](https://travis-ci.org/scalalandio/ocdquery.svg?branch=master)](https://travis-ci.org/scalalandio/ocdquery)
[![Maven Central](https://img.shields.io/maven-central/v/io.scalaland/ocdquery-core_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Cocdquery)
[![License](http://img.shields.io/:license-Apache%202-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

[Doobie](https://github.com/tpolecat/doobie/) queries generated using higher-kinded data.

## Instalation

1. add in your sbt:
```scala
libraryDependencies += "io.scalaland" %% "ocdquery-core" % "0.4.1"
```
(and maybe some optics library like [Quicklens](https://github.com/softwaremill/quicklens)
or [Monocle](https://github.com/julien-truffaut/Monocle))

2. create higher-kinded data representation:

```scala
import java.time.LocalDate
import java.util.UUID

final case class TicketF[F[_], C[_]](
  id:      C[UUID],
  name:    F[String],
  surname: F[String],
  from:    F[String],
  to:      F[String],
  date:    F[LocalDate]
)
```

3. create a repository:

```scala
import cats.Id
import com.softwaremill.quicklens._
import doobie.h2.implicits._
import io.scalaland.ocdquery._

// only have to do it once!
val TicketRepo: Repo.EntityRepo[TicketF] = {
  // I have no idea why shapeless cannot find this Generic on its own :/
  // if you figure it out, please PR!!!
  implicit val ticketRead: doobie.Read[Repo.ForEntity[TicketF]#Entity] =
    QuasiAuto.read(shapeless.Generic[TicketF[Id, Id]])
    
  Repo.forEntity[TicketF](
    "tickets".tableName,
    // I suggest using quicklens or monocle's extension methods
    // as they are more reliable than .copy
    DefaultColumnNames.forEntity[TicketF].modify(_.from).setTo("from_".columnName)
  )
}
```

4. generate queries

```scala
// build these in you services with type safety!

TicketRepo.insert(
  // no need to pass "empty" fields like "id = Unit"!
  Create.fromTuple(("John", "Smith", "London", "Paris", LocalDate.now))
).run

import io.scalaland.ocdquery.sql._ // common filter syntax like `=`, `<>`

TicketRepo.update.withFilter { columns =>
  (columns.name `=` "John") and (columns.surname `=` "Smith")
}(
  TicketRepo.emptyUpdate.modify(_.data).setTo(LocalDate.now)
).run

TicketRepo.fetch.withSort(_.name, Sort.Ascending).withLimit(5) {
 _.from `=` "London"
}.to[List]

TicketRepo.delete(_.id `=` deletedId).run
```

5. perform even joins returning tuples of entities:

```scala
val joiner = TicketRepo
  .join(TicketRepo).on(_._1.id, _._2.id) // after .join() we have a tuple!
  .join(TicketRepo).on(_._2.id, _._3.id) // and now triple!
  .fetch.withSort(_._1.name, Sort.Ascending).withLimit(5) { columns =>
    columns._1.name `=` "John"
  }.to[List] // ConnectionIO[(Entity, Entity, Entity)]
```

## Limitations

* Library assumes that `EntityF` is flat, and automatic generation of Doobie queries is done in a way which doesn't
  allow you to use JOINs, nested SELECTs etc. If you need them you can use utilities from `RepoMeta` to write your own
  query, while delegating some of the work to `RepoMeta` (see how `Repo` does it!). 
* Using `EntityF` everywhere is definitely not convenient. Also it doesn't let you
  define default values like e.g. `None`/`Skipped` for optional fields. So use them
  internally, as entities to work with your database and separate them from
  entities exposed in your API/published language. You can use [chimney](https://github.com/scalalandio/chimney)
  for turning public instances to and from internal instances,
* types sometimes confuse compiler, so while it can derive something like `shapeless.Generic[TicketF[Id, Id]]`,
  it has issues finding `Generic.Aux`, so Doobie sometimes get's confused - `QuasiAuto` let you provide
  the right values explicitly, so that the derivation is not blocked by such silly issue. 
