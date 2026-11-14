package com.innova.captuia.data.schedule

import androidx.room.withTransaction
import com.innova.captuia.data.folder.FolderSortOrder
import com.innova.captuia.data.folder.toDomain
import com.innova.captuia.data.local.CaptuDatabase
import com.innova.captuia.data.local.ClassSessionEntity
import com.innova.captuia.data.local.FolderEntity
import com.innova.captuia.domain.folder.FolderNameValidator
import com.innova.captuia.domain.model.ClassSession
import com.innova.captuia.domain.model.FolderKind
import com.innova.captuia.domain.model.FolderSource
import com.innova.captuia.domain.model.Subject
import com.innova.captuia.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek

/** Horario guardado en Room. Cada materia es una carpeta de tipo SUBJECT con sus franjas. */
class DefaultScheduleRepository(
    private val database: CaptuDatabase,
) : ScheduleRepository {

    private val folderDao = database.folderDao()
    private val scheduleDao = database.scheduleDao()
    private val captureDao = database.captureDao()

    override fun observeSubjects(): Flow<List<Subject>> =
        combine(
            folderDao.observeByKind(FolderKind.SUBJECT.name),
            scheduleDao.observeAll(),
        ) { folders, sessions -> toSubjects(folders, sessions) }

    override suspend fun subjects(): List<Subject> =
        toSubjects(folderDao.byKind(FolderKind.SUBJECT.name), scheduleDao.all())

    override suspend fun allFolderNames(): List<String> = folderDao.all().map { it.name }

    override suspend fun saveSubject(folderId: Long?, name: String, sessions: List<ClassSession>): Long =
        database.withTransaction {
            val nameKey = FolderNameValidator.key(name)
            val id = if (folderId == null) {
                folderDao.insert(
                    FolderEntity(
                        name = name,
                        nameKey = nameKey,
                        kind = FolderKind.SUBJECT.name,
                        systemKey = null,
                        sortOrder = FolderSortOrder.SUBJECT,
                    ),
                )
            } else {
                folderDao.rename(folderId, FolderKind.SUBJECT.name, name, nameKey)
                scheduleDao.deleteForFolder(folderId)
                folderId
            }
            scheduleDao.insertAll(
                sessions.map { session ->
                    ClassSessionEntity(
                        folderId = id,
                        dayOfWeek = session.dayOfWeek.value,
                        startMinutes = session.startMinutes,
                        endMinutes = session.endMinutes,
                    )
                },
            )
            id
        }

    override suspend fun deleteSubject(folderId: Long) {
        database.withTransaction {
            captureDao.releaseFromFolder(folderId, FolderSource.NONE.name)
            // Las franjas se eliminan en cascada con la carpeta.
            folderDao.delete(folderId, FolderKind.SUBJECT.name)
        }
    }

    private fun toSubjects(folders: List<FolderEntity>, sessions: List<ClassSessionEntity>): List<Subject> {
        val byFolder = sessions.groupBy { it.folderId }
        return folders.map { folder ->
            Subject(
                folder = folder.toDomain(),
                sessions = byFolder[folder.id].orEmpty().map { session ->
                    ClassSession(
                        dayOfWeek = DayOfWeek.of(session.dayOfWeek),
                        startMinutes = session.startMinutes,
                        endMinutes = session.endMinutes,
                    )
                },
            )
        }
    }
}
