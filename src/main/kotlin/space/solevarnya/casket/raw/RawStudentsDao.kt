package space.solevarnya.casket.raw

import space.solevarnya.casket.Dao
import space.solevarnya.casket.ReadAll

@Dao(RawStudentEntity::class)
interface RawStudentsDao {
    @ReadAll
    fun readAll(): List<RawStudentEntity>
}