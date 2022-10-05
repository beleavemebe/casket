package space.solevarnya.casket.raw

import space.solevarnya.casket.Entity

@Entity(tableFileName = "names.txt")
data class RawStudentEntity(
    val name: String,
    val surname: String,
    val patronymic: String,
)
