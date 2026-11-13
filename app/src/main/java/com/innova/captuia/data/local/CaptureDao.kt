package com.innova.captuia.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {

    @Query("SELECT * FROM captures ORDER BY dateAddedMillis DESC, mediaId DESC")
    fun observeAll(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE mediaId = :id")
    fun observeById(id: Long): Flow<CaptureEntity?>

    @Query("SELECT mediaId FROM captures")
    suspend fun allIds(): List<Long>

    /** Inserta solo las capturas que aún no están en el índice. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNew(items: List<CaptureEntity>)

    @Query("DELETE FROM captures WHERE mediaId IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
