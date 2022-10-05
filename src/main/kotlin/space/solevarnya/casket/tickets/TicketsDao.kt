package space.solevarnya.casket.tickets

import space.solevarnya.casket.Create
import space.solevarnya.casket.Dao
import space.solevarnya.casket.Read
import space.solevarnya.casket.ReadAll

@Dao(TicketEntity::class)
interface TicketsDao {
    @Create
    fun create(entity: TicketEntity)

    @ReadAll
    fun readAll(): List<TicketEntity>

    @Read
    fun read(id: Int): TicketEntity
}