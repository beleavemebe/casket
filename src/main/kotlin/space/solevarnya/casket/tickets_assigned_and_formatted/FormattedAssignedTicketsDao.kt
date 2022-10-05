package space.solevarnya.casket.tickets_assigned_and_formatted

import space.solevarnya.casket.Create
import space.solevarnya.casket.Dao

@Dao(FormattedAssignedTicketEntity::class)
interface FormattedAssignedTicketsDao {
    @Create
    fun create(entity: FormattedAssignedTicketEntity)
}