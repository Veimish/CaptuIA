package com.innova.captuia.data.folder

import androidx.room.withTransaction
import com.innova.captuia.data.local.CaptuDatabase
import com.innova.captuia.data.local.FolderEntity
import com.innova.captuia.domain.folder.FolderNameCheck
import com.innova.captuia.domain.folder.FolderNameValidator
import com.innova.captuia.domain.model.Folder
import com.innova.captuia.domain.model.FolderKind
import com.innova.captuia.domain.model.FolderSource
import com.innova.captuia.domain.model.FolderSummary
import com.innova.captuia.domain.model.SystemFolder
import com.innova.captuia.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Carpetas guardadas en Room. [systemFolderNames] da el nombre visible de cada
 * carpeta del sistema (viene de los textos de la app).
 */
class DefaultFolderRepository(
    private val database: CaptuDatabase,
    private val systemFolderNames: Map<SystemFolder, String>,
) : FolderRepository {

    private val folderDao = database.folderDao()
    private val captureDao = database.captureDao()

    override fun observeSummaries(): Flow<List<FolderSummary>> =
        folderDao.observeWithCounts().map { rows ->
            rows.map { FolderSummary(folder = it.folder.toDomain(), captureCount = it.captureCount) }
        }

    override fun observeById(id: Long): Flow<Folder?> =
        folderDao.observeById(id).map { it?.toDomain() }

    override suspend fun ensureSystemFolders() {
        folderDao.insertIgnoring(
            SystemFolder.entries.map { key ->
                val name = systemFolderNames.getValue(key)
                FolderEntity(
                    name = name,
                    nameKey = FolderNameValidator.key(name),
                    kind = FolderKind.SYSTEM.name,
                    systemKey = key.name,
                    sortOrder = key.ordinal,
                )
            },
        )
    }

    override suspend fun systemFolderId(key: SystemFolder): Long =
        folderDao.idOfSystemFolder(key.name)
            ?: run {
                ensureSystemFolders()
                checkNotNull(folderDao.idOfSystemFolder(key.name)) { "Falta la carpeta del sistema $key" }
            }

    override suspend fun create(name: String): FolderNameCheck = database.withTransaction {
        val check = FolderNameValidator.check(name, folderDao.all().map { it.name })
        if (check is FolderNameCheck.Valid) {
            folderDao.insert(
                FolderEntity(
                    name = check.name,
                    nameKey = FolderNameValidator.key(check.name),
                    kind = FolderKind.CUSTOM.name,
                    systemKey = null,
                    sortOrder = FolderSortOrder.CUSTOM,
                ),
            )
        }
        check
    }

    override suspend fun rename(id: Long, name: String): FolderNameCheck = database.withTransaction {
        val current = folderDao.byId(id)
        if (current == null || current.kind != FolderKind.CUSTOM.name) {
            // Solo se renombran aquí las carpetas del usuario; las materias, desde el horario.
            return@withTransaction FolderNameCheck.Empty
        }
        val check = FolderNameValidator.check(
            input = name,
            existingNames = folderDao.all().map { it.name },
            currentName = current.name,
        )
        if (check is FolderNameCheck.Valid) {
            folderDao.rename(id, FolderKind.CUSTOM.name, check.name, FolderNameValidator.key(check.name))
        }
        check
    }

    override suspend fun delete(id: Long) {
        database.withTransaction {
            val folder = folderDao.byId(id)
            if (folder != null && folder.kind == FolderKind.CUSTOM.name) {
                captureDao.releaseFromFolder(id, FolderSource.NONE.name)
                folderDao.delete(id, FolderKind.CUSTOM.name)
            }
        }
    }

}

/** Posición de cada tipo de carpeta en las listas: sistema (0 a 3), materias y carpetas del usuario. */
object FolderSortOrder {
    const val SUBJECT = 50
    const val CUSTOM = 100
}

fun FolderEntity.toDomain() = Folder(
    id = id,
    name = name,
    kind = FolderKind.entries.firstOrNull { it.name == kind } ?: FolderKind.CUSTOM,
    systemKey = SystemFolder.entries.firstOrNull { it.name == systemKey },
)
