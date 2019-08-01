# Over-Complicated Database Query (OCD Query)

[![Build Status](https://travis-ci.org/scalalandio/ocdquery.svg?branch=master)](https://travis-ci.org/scalalandio/ocdquery)
[![Maven Central](https://img.shields.io/maven-central/v/io.scalaland/ocdquery-core_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Cocdquery)
[![License](http://img.shields.io/:license-Apache%202-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

Doobie queries generated using higher-kinded data.

## Instalation

1. add in your sbt:
```scala
libraryDependencies += "io.scalaland" %% "ocdquery-core" % "0.3.0"
```

2. create kigner-kinded data representation:

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
import doobie._
import doobie.implicits._
import io.scalaland.ocdquery._

// only have to do it once!
object TicketRepo extends Repo(
   RepoMeta.forEntity("tickets",
                      TicketF[ColumnNameF, ColumnNameF](
                        id      = "id",
                        name    = "name",
                        surname = "surname",
                        from    = "from_",
                        to      = "to",
                        date    = "date"
                      ))
)

// but you can make your life even easier with a bit of default magic
// and lenses (they don't mess up type inference like .copy does)

// (lenses, not provided, pick and add them on your own!)
// (both monocle with macro syntax and quicklenses are nice!)
import com.softwaremill.quicklens._

object TicketRepo extends Repo(
   RepoMeta.forEntity("tickets",
                      DefaultColumnNames.forEntity[TicketF].modify(_.from).setTo("from_")
   )
)
```

4. generate queries

```scala
// build these in you services with type safety!

TicketRepo.update(
  select = TicketF[Selectable, Selectable](
    id       = Skipped,
    name:    = Fixed("John"),
    surname: = Fixed("Smith"),
    from:    = Skipped,
    to:      = Skipped,
    date:    = Skipped
  ),
  update = TicketF[Selectable, Selectable](
    id       = Skipped,
    name:    = Skipped,
    surname: = Skipped,
    from:    = Skipped,
    to:      = Skipped,
    date:    = Fixed(LocalDate.now)
  )
).run

TicketRepo.fetch(
  select = TicketF[Selectable, Selectable](
    id       = Skipped,
    name:    = Skipped,
    surname: = Skipped,
    from:    = Fixed("London"),
    to:      = Skipped,
    date:    = Skipped
  ),
  sort = Some("name" -> Sort.Ascending),
  limit = Some(5)
).to[List]

// or (with defaults and lenses)

// (lenses, not provided, pick and add them on your own!)
import com.softwaremill.quicklens._

TicketRepo.update(
  select = TicketRepo.emptySelect
    .modify(_.name).setTo("John")
    .modify(_.surname).setTo("Smith"),
  update = TicketRepo.emptySelect
    .modify(_.data).setTo(LocalDate.now)
).run

TicketRepo.fetch(
  select = TicketRepo.emptySelect
    .modify(_.from).setTo("London"),
  sort = Some("name" -> Sort.Ascending),
  limit = Some(5)
).to[List]
```

5. perform even joins returning tuples of entities:

```scala
import TicketRepo.meta.Names

val joiner = TicketRepo
  .join(TicketRepo, (TicketRepo.col(_.id), TicketRepo.col(_.id)))
  .join(TicketRepo, (_._2.id, TicketRepo.col(_.id)))

joiner.fetch((filter1, filter2, filter2),
             sort = Some(((_: (Names, Names, Names))._1.name) -> Sort.Ascending),
             offset = None,
             limit = Some(5)).to[List] // ConnectionIO[(Entity, Entity, Entity)]
```

## How does it exactly works?

### Initial idea

Imagine you wanted to generate queries from data objects.

```scala
import java.util.UUID
import java.time.LocalDate

final case class Ticket(
  id:      UUID,
  name:    String,
  surname: String,
  from:    String,
  to:      String,
  date:    LocalDate
)


import doobie._
import doobie.implicits._

object TicketRepo {
  // ...
}

```

You may want to be able to insert new entity:

```scala
final case class TicketCreate(
  id:      Unit,   // placeholder as this is created by database
  name:    String,
  surname: String,
  from:    String,
  to:      String,
  date:    LocalDate
)

object TicketRepository {
   // ...
   def insert(entity: TicketCreate): Update0 = ???
   // ...
}
```

You may want to be able to fetch data using some obligatory part of the fetching (e.g. ID)
and maybe some optional parts (having other field equal to some value):

```scala
final case class TicketFilter(
  id:      Option[UUID],     // where id      = [this value if set]
  name:    Option[String],   // and   name    = [this value if set]
  surname: Option[String],   // and   surname = [this value if set]
  from:    Option[String],   // and   from    = [this value if set]
  to:      Option[String],   // and   to      = [this value if set]
  date:    Option[LocalDate] // and   data    = [this value if set]
)

object TicketRepository {
   // ...
   def update(entityFind: TicketFilter): Query0[Ticket] = ???
   // ...
}
```

You might want to update existing entity using case class - this you make building queries easy.

```scala
final case class TicketUpdate(
  id:      Option[Unit],     // set uuid    = [this value if set],
  name:    Option[String],   //     name    = [this value if set],
  surname: Option[String],   //     surname = [this value if set],
  from:    Option[String],   //     from    = [this value if set],
  to:      Option[String],   //     to      = [this value if set],
  date:    Option[LocalDate] //     data    = [this value if set]
)

object TicketRepository {
   // ...
   def update(entityUpdate: TicketUpdate): Update0 = ???
   // ...
}
```

And you might want to delete entity using fetch criteria:

```scala
object TicketRepository {
   // ...
   def delete(entityDelete: TicketFilter): Query0[Ticket] = ???
   // ...
}
```

In all these cases having some object that defines how query will be performed
might be really useful. We could get a lot of queries almost for free.

All these case classes can easily get out of sync, so it would be good
if something could enforce that adding field in one place will require adding
it somewhere else.

If we operated under that assumption, we would be able to derive queries
automatically. That is as long as we had two more pieces of information
table name and:

```scala
final case class TicketColumns(
  id:      String,
  name:    String,
  surname: String,
  from:    String,
  to:      String,
  date:    String
)
```

### Refactoring idea

If we take a closer look, we'll see that we have 3 case classes that are
virtually identical - if we were able to flip the type of some fields from
`A` to `Option[A]` to turn entity to update, or all of them to `String`
to turn it into column configuration, we would reduce the amount of code
and fixed issue of case class synchronization.

And we can do this! The idea is called higher-kinded data and looks like this:

```scala
import java.time.LocalDate
import java.util.UUID

type Id[A] = A
type UnitF[A] = Unit
type ColumnNameF[A] = String

// F is for normal columns which should be available in some what for all lifecycle
// C is for these that should be empty during creation and available from then on
final case class TicketF[F[_], C[_]](
  id:      C[UUID],
  name:    F[String],
  surname: F[String],
  from:    F[String],
  to:      F[String],
  date:    F[LocalDate]
)

type TicketCreate  = TicketF[Id, UnitF] // C[_] fields are Units, the rest as of type inside of F[_]
type Ticket        = TicketF[Id, Id] // all fields are of the type inside of F[_]/C[_]
type TicketFilter  = TicketF[Option, Option] // all fields are of Option of inside of F[_]/C[_]
type TicketColumns = TicketF[ColumnNameF, ColumnNameF] // all fields are Strings
```

Higher-kinded data is data with higher-kinded types as type parameters.

This library is about taking this higher-kinded data definition and generating
basic CRUD queries for it.

### Implementation

During implementation some decisions had to be made:

 * instead of `Option` we use our own `Selectable` type which could be `Fixed(to)` or `Skipped `
   to avoid issues during derivation that would occur if you had e.g. `O[Option[String]]`
   as one of field types,
 * derivation metadata is stored inside `RepoMeta[EntityF]` instance - you can reuse
   on your own, if the default autogenerated queries doesn't suit your use case:
   ```scala
   import doobie._
   import doobie.implicits._
   import io.scalaland.ocdquery._

   val ticketRepoMeta =
      RepoMeta.forEntity("tickets",
                         TicketF[ColumnNameF, ColumnNameF](
                           id      = "id",
                           name    = "name",
                           surname = "surname",
                           from    = "from_",
                           to      = "to",
                           date    = "date"
                         ))
   ```
 * if however default way suit your taste feel free to use `Repo` implementation!
   ```scala
   object TicketRepo extends Repo(ticketRepoMeta)
   
   val ticketCreate = TicketF[Id, UnitF](
     id      = (),
     name    = "John",
     surname = "Smith",
     from    = "New York",
     to      = "London",
     date    = LocalDate.now()
   )
  
   val byName = TicketF[Selectable, Selectable](
     id      = Skipped,
     name    = Fixed(ticketCreate.name),
     surname = Fixed(ticketCreate.surname),
     from    = Skipped,
     to      = Skipped,
     date    = Skipped
   )

   val update = TicketF[Selectable, Selectable](
     id      = Skipped,
     name    = Fixed("Jane"),
     surname = Skipped,
     from    = Fixed("London"),
     to      = Fixed("New York"),
     date    = Skipped
   )
   
   for {
     // that's how you can insert data
     _ <- TicketRepo.insert(ticketCreate).run
     // that's how you can fetch existing data
     ticket <- TicketRepo.fetch(byName).unique
     // that's how you can update existing data
     _ <- TicketRepo.update(byName, update).run
     updatedTicket <- TicketRepo.fetch(fetch).unique
     // this is how you can delete existing data
     _ <- TicketRepo.delete(byName).run
   } yield ()
   ```
 * to avoid writing `EntityF[Id, Id]`, `EntityF[Selectable, Selectable]` and `EntityF[Id, UnitF]`
   manually, some type aliases were introduced:
   ```scala
   import io.scalaland.ocdquery._

   type TicketCreate = TicketRepo.meta.Create
   type Ticket       = TicketRepo.meta.Entity
   type TicketSelect = TicketRepo.meta.Select
   ```

## Limitations

* Library assumes that `EntityF` is flat, and automatic generation of Doobie queries is done in a way which doesn't
  allow you to use JOINs, nested SELECTs etc. If you need them you can use utilities from `RepoMeta` to write your own
  query, while delegating some of the work to `RepoMeta` (see how `Repo` does it!). 
* Using `EntityF` everywhere is definitely not convenient. Also it doesn't let you
  define default values like e.g. `None`/`Skipped` for optional fields. So use them
  internally, as entities to work with your database and separate them from
  entities exposed in your API/published language. You can use [chimney](https://github.com/scalalandio/chimney)
  for turning public instances to and from internal instances.  
