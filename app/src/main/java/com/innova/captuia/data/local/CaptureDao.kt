package com.innova.captuia.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
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

    /** Búsqueda por texto; la consulta se arma en el repositorio según las palabras. */
    @RawQuery(observedEntities = [CaptureEntity::class])
    fun search(query: SupportSQLiteQuery): Flow<List<CaptureEntity>>

    @Query("SELECT COUNT(*) FROM captures")
    fun observeTotal(): Flow<Int>

    @Query("SELECT COUNT(*) FROM captures WHERE textStatus = :status")
    fun observeCountByStatus(status: String): Flow<Int>

    @Query(
        "SELECT * FROM captures WHERE textStatus = :status " +
            "ORDER BY dateAddedMillis DESC, mediaId DESC LIMIT :limit",
    )
    suspend fun byStatus(status: String, limit: Int): List<CaptureEntity>

    @Query(
        "UPDATE captures SET textStatus = :status, recognizedText = :text, searchText = :searchText " +
            "WHERE mediaId = :id",
    )
    suspend fun updateText(id: Long, status: String, text: String?, searchText: String?)
}
