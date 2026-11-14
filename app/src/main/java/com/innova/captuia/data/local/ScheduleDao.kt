package com.innova.captuia.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM class_sessions ORDER BY dayOfWeek, startMinutes")
    fun observeAll(): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions ORDER BY dayOfWeek, startMinutes")
    suspend fun all(): List<ClassSessionEntity>

    @Insert
    suspend fun insertAll(sessions: List<ClassSessionEntity>)

    @Query("DELETE FROM class_sessions WHERE folderId = :folderId")
    suspend fun deleteForFolder(folderId: Long)
}
