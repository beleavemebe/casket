package space.solevarnya.casket.tickets

import space.solevarnya.casket.Entity
import space.solevarnya.casket.PrimaryKey

@Entity(tableFileName = "tickets.txt")
data class TicketEntity(
    @PrimaryKey val id: Int,
    val filePath: String,
)
