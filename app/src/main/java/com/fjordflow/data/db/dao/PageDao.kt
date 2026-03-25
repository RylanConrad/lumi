package com.fjordflow.data.db.dao

import androidx.room.*
import com.fjordflow.data.db.entity.PageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {
    @Query("SELECT * FROM pages WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun getPagesForBook(bookId: Int): Flow<List<PageEntity>>

    @Query("SELECT MAX(pageNumber) FROM pages WHERE bookId = :bookId")
    suspend fun getMaxPageNumber(bookId: Int): Int?

    @Insert
    suspend fun insertPage(page: PageEntity): Long

    @Delete
    suspend fun deletePage(page: PageEntity)

    @Update
    suspend fun updatePage(page: PageEntity)
}
