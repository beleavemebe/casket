package space.solevarnya.casket.tickets_assigned_and_formatted

import space.solevarnya.casket.Entity
import space.solevarnya.casket.PrimaryKey

@Entity(tableFileName = "tickets_assigned_and_formatted.txt")
data class FormattedAssignedTicketEntity(
    @PrimaryKey val studentFullName: String,
    val ticketPath: String,
)
