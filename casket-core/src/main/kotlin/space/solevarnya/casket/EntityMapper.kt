package space.solevarnya.casket

interface EntityMapper<E> {
    fun mapToString(entity: E): String
    fun mapToEntity(string: String): E
}