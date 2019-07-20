# Over-Complicated Database Query (OCD Query)

[![Build Status](https://travis-ci.org/scalalandio/ocdquery.svg?branch=master)](https://travis-ci.org/scalalandio/ocdquery)
[![Maven Central](https://img.shields.io/maven-central/v/io.scalaland/ocdquery-core_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Cocdquery)
[![License](http://img.shields.io/:license-Apache%202-green.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

Doobie queries generated using higher-kinded data.

## What does it mean?

### Initial idea

Imagine you wanted to generate queries from data objects.

You may want to be able to insert new entity:

```scala
import java.util.UUID
import java.time.LocalDate

import doobie._
import doobie.implicits._

final case class Ticket(
  id:      UUID,
  name:    String,
  surname: String,
  from:    String,
  to:      String,
  date:    LocalDate
)

object TicketRepository {
   // ...
   def insert(entity: Ticket): Update0 = ???
   // ...
}
```

You may want to be able to fetch data using some obligatory part of the fetching (e.g. ID)
and maybe some optional parts (having other field equal to some value):

```scala
import java.util.UUID
import java.time.LocalDate

import doobie._
import doobie.implicits._

final case class TicketFilter(
  id:      UUID,             // where id = [this value]
  name:    Option[String],   // and name    = [this value if set]
  surname: Option[String],   // and surname = [this value if set]
  from:    Option[String],   // and from    = [this value if set]
  to:      Option[String],   // and to      = [this value if set]
  date:    Option[LocalDate] // and data    = [this value if set]
)

object TicketRepository {
   // ...
   def update(entityFind: TicketFilter): Query0[Ticket] = ???
   // ...
}
```

You might want to update existing entity using case class - this you make building queries easy.

```scala
import java.util.UUID
import java.time.LocalDate

import doobie._
import doobie.implicits._

final case class TicketUpdate(
  id:      UUID,             // where id = [this value]
  name:    Option[String],   // set name    = [this value if set],
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
type ColumnNameF[A] = String

// O and S stands for Obligatory and Selectable respectively
final case class TicketF[O[_], S[_]](
  id:      O[UUID],
  name:    S[String],
  surname: S[String],
  from:    S[String],
  to:      S[String],
  date:    S[LocalDate]
)

type Ticket = TicketF[Id, Id]
type TicketFilter = TicketF[Id, Option]
type TicketColumns = TicketF[ColumnNameF, ColumnNameF]
```

Higher-kinded data is data with higher-kinded types as type parameters.

This library is about taking this higher-kinded data definition and generating
basic CRUD queries for it.

### Implementation

During implementation some decisions had to be made:

 * instead of `Option` we use our own `Selectable` type which could be `Fixed(to)` or `Skipped `
   to avoid issues during derivation that would occur if you had e.g. `O[Option[String]]`
   as one of field types,
 * to avoid writing `EntityF[Id, Id]`, `EntityF[Id, Selectable]` and `EntityF[ColumnNameF, ColumnNameF]`
   manually, some type aliases were introduced:
   ```scala
   import io.scalaland.ocdquery._

   type Ticket = EntityOf[TicketF]
   type TicketUpdate = SelectOf[TicketF]
   type TicketFilter = SelectOf[TicketF]
   type TicketColumns = ColumnsOf[TicketF]
   ```
 * derivation metadata is stored inside `RepoMeta[EntityF]` instance - you can reuse
   on your own, if the default autogenerated queries doesn't suit your use case:
   ```scala
   import io.scalaland.ocdquery._

   val ticketRepoMeta =
      RepoMeta.instanceFor("tickets",
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
   import io.scalaland.ocdquery._

   object TicketRepo extends Repo(ticketRepoMeta)
   
   val ticket = TicketF[Id, Id](
     id      = UUID.randomUUID(),
     name    = "John",
     surname = "Smith",
     from    = "New York",
     to      = "London",
     date    = LocalDate.now()
   )
  
   val fetch = TicketF[Id, Selectable](
     id      = ticket.id,
     name    = Skipped,
     surname = Skipped,
     from    = Skipped,
     to      = Skipped,
     date    = Skipped
   )

   val update = TicketF[Id, Selectable](
     id      = ticket.id,
     name    = Fixed("Jane"),
     surname = Skipped,
     from    = Fixed("London"),
     to      = Fixed("New York"),
     date    = Skipped
   )
   
   for {
     // that's how you can insert data
     _ <- TicketRepo.insert(ticket).run
     // that's how you can fetch existing data
     fetchedTicket <- TicketRepo.fetch(fetch).unique
     // that's how you can update existing data
     _ <- TicketRepo.update(update).run
     updatedTicket <- TicketRepo.fetch(fetch).unique
     // this is how you can delete existing data
     _ <- TicketRepo.delete(fetch).run
   } yield ()
   ```

## Limitations

* Some assumptions are made: fields are grouped into Obligatory and Selectable.
  Obligatory fields are immutable and have to be present in all filters. They are
  great candidates for usage for IDs. All the other fields should be Selectable,
  which means that they are used for filtering in `SELECT` and `DELETE` and
  allow for selecting which fields are updated in `UPDATE`. If this is not aligning
  with your flow you will not use `Repo` but you can use `RepoMeta` to decide
  on your own how you want to use them.
* Using `EntityF` everywhere is definitely not convenient. Also it doesn't let you
  define default values like e.g. `None`/`Skipped` for optional fields. So use them
  internally, as entities to work with your database and separate them from
  entities exposed in your API/published language. You can use [chimney](https://github.com/scalalandio/chimney)
  for turning public instances to and from internal instances.  
