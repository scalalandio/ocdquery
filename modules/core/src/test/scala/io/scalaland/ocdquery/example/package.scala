package io.scalaland.ocdquery

package object example {

  type Ticket = EntityOf[TicketF]

  type TicketSelect = SelectOf[TicketF]

  type TicketColumns = ColumnsOf[TicketF]
}
