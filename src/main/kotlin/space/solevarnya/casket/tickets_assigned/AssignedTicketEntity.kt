package space.solevarnya.casket.tickets_assigned

import space.solevarnya.casket.Entity
import space.solevarnya.casket.PrimaryKey

@Entity(tableFileName = "assigned_tickets.txt")
data class AssignedTicketEntity(
    @PrimaryKey val studentId: Int,
    val ticketId: Int,
)
