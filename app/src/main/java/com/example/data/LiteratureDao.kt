package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LiteratureDao {
    @Query("SELECT * FROM literature_items ORDER BY updatedAt DESC, createdAt DESC")
    fun observeAll(): Flow<List<LiteratureItemEntity>>

    @Query("SELECT * FROM literature_items ORDER BY updatedAt DESC, createdAt DESC")
    suspend fun getAllOnce(): List<LiteratureItemEntity>

    @Query("SELECT COUNT(*) FROM literature_items")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: LiteratureItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<LiteratureItemEntity>)

    @Query("DELETE FROM literature_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM literature_items")
    suspend fun deleteAll()

    @Query("UPDATE literature_items SET memorized = :memorized, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setMemorized(id: String, memorized: Boolean, updatedAt: Long)

    @Transaction
    suspend fun replaceAll(items: List<LiteratureItemEntity>) {
        deleteAll()
        if (items.isNotEmpty()) {
            upsertAll(items)
        }
    }
}
