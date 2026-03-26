package com.fjordflow.data.db.dao

import androidx.room.*
import com.fjordflow.data.db.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Int): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("UPDATE books SET lastPageIndex = :pageIndex WHERE id = :bookId")
    suspend fun updateLastPage(bookId: Int, pageIndex: Int)

    @Delete
    suspend fun deleteBook(book: BookEntity)
}
