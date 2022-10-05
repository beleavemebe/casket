package space.solevarnya.casket

interface KeyMatcher<Entity, Key> {
    fun keyMatchesEntity(key: Key, entity: Entity): Boolean
}

fun <Entity, Key> createDummyKeyMatcher(): KeyMatcher<Entity, Key> {
    return object : KeyMatcher<Entity, Key> {
        override fun keyMatchesEntity(key: Key, entity: Entity) = error("Dummy")
    }
}
