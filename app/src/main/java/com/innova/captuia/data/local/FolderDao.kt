package com.innova.captuia.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query(
        "SELECT f.*, (SELECT COUNT(*) FROM captures c WHERE c.folderId = f.id) AS captureCount " +
            "FROM folders f ORDER BY f.sortOrder, f.name COLLATE NOCASE",
    )
    fun observeWithCounts(): Flow<List<FolderWithCount>>

    @Query("SELECT * FROM folders WHERE id = :id")
    fun observeById(id: Long): Flow<FolderEntity?>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun byId(id: Long): FolderEntity?

    @Query("SELECT * FROM folders")
    suspend fun all(): List<FolderEntity>

    @Query("SELECT id FROM folders WHERE systemKey = :systemKey")
    suspend fun idOfSystemFolder(systemKey: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoring(folders: List<FolderEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(folder: FolderEntity): Long

    @Query("UPDATE folders SET name = :name, nameKey = :nameKey WHERE id = :id AND systemKey IS NULL")
    suspend fun rename(id: Long, name: String, nameKey: String)

    @Query("DELETE FROM folders WHERE id = :id AND systemKey IS NULL")
    suspend fun deleteCustom(id: Long)
}
