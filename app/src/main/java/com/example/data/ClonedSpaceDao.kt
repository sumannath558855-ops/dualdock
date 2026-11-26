package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClonedSpaceDao {
    @Query("SELECT * FROM cloned_spaces ORDER BY lastLaunchedAt DESC, createdAt DESC")
    fun observeAll(): Flow<List<ClonedSpace>>

    @Query("SELECT * FROM cloned_spaces WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ClonedSpace?

    @Query("SELECT slotIndex FROM cloned_spaces WHERE engineType = 'WEB_CONTAINER' AND slotIndex > 0")
    suspend fun getUsedContainerSlots(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(space: ClonedSpace): Long

    @Update
    suspend fun update(space: ClonedSpace)

    @Query("UPDATE cloned_spaces SET lastLaunchedAt = :timestamp WHERE id = :id")
    suspend fun updateLastLaunched(id: Long, timestamp: Long)

    @Query("UPDATE cloned_spaces SET isLocked = :locked WHERE id = :id")
    suspend fun updateLocked(id: Long, locked: Boolean)

    @Query("DELETE FROM cloned_spaces WHERE id = :id")
    suspend fun deleteById(id: Long)
}
