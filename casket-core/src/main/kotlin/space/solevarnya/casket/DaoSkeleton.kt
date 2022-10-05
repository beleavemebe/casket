@file:Suppress("unused")
package space.solevarnya.casket

import java.io.File
import java.nio.file.Files.lines

class DaoSkeleton<Entity, Key>(
    tableFilePath: String,
    private val entityMapper: EntityMapper<Entity>,
    private val keyMatcher: KeyMatcher<Entity, Key> = createDummyKeyMatcher()
) {
    private val tableFile = File(tableFilePath)

    init {
        if (tableFile.exists().not()) {
            tableFile.createNewFile()
        }
    }

    fun readEntity(key: Key): Entity {
        return readEntities().first { keyMatcher.keyMatchesEntity(key, it) }
    }

    fun readEntities(): List<Entity> {
        return lines(tableFile.toPath()).map(entityMapper::mapToEntity).toList()
    }

    fun appendEntity(entity: Entity) {
        if (entity in readEntities()) return
        val prefix = if (tableFile.length() == 0L) "" else "\n"
        tableFile.appendText(prefix + entityMapper.mapToString(entity))
    }

    fun appendEntity(key: Key, entity: Entity) {
        runCatching {
            val readEntity = readEntity(key)
            readEntity
        }.getOrElse {
            appendEntity(entity)
        }
    }
}
