package com.fjordflow.data.db.dao

import androidx.room.*
import com.fjordflow.data.db.entity.FlashCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashCardDao {
    @Query("SELECT * FROM flashcards ORDER BY dueDate ASC")
    fun getAllCards(): Flow<List<FlashCardEntity>>

    @Query("SELECT * FROM flashcards WHERE dueDate <= :now ORDER BY dueDate ASC")
    fun getDueCards(now: Long = System.currentTimeMillis()): Flow<List<FlashCardEntity>>

    @Query("SELECT COUNT(*) FROM flashcards")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards WHERE dueDate <= :now")
    fun getDueCount(now: Long = System.currentTimeMillis()): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: FlashCardEntity): Long

    @Update
    suspend fun updateCard(card: FlashCardEntity)

    @Delete
    suspend fun deleteCard(card: FlashCardEntity)
}
