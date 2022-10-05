package space.solevarnya.casket.students

import space.solevarnya.casket.Entity
import space.solevarnya.casket.PrimaryKey

@Entity(tableFileName = "students.txt")
data class StudentEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val surname: String,
    val patronymic: String,
) {
    val fullName: String get() = "$name $surname $patronymic"
}

