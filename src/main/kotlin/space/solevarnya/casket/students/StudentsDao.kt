package space.solevarnya.casket.students

import space.solevarnya.casket.Create
import space.solevarnya.casket.Dao
import space.solevarnya.casket.Read
import space.solevarnya.casket.ReadAll

@Dao(StudentEntity::class)
interface StudentsDao {
    @Create
    fun create(entity: StudentEntity)

    @ReadAll
    fun readAll(): List<StudentEntity>

    @Read
    fun read(id: Int): StudentEntity
}