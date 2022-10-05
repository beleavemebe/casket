package space.solevarnya.casket.tickets_assigned

import space.solevarnya.casket.Create
import space.solevarnya.casket.Dao
import space.solevarnya.casket.ReadAll

@Dao(AssignedTicketEntity::class)
interface AssignedTicketsDao {
    @Create
    fun create(ticket: AssignedTicketEntity)

    @ReadAll
    fun readAll(): List<AssignedTicketEntity>
}