package com.fjordflow.data.repository

import com.fjordflow.data.db.dao.BookDao
import com.fjordflow.data.db.entity.BookEntity
import kotlinx.coroutines.flow.Flow

class BookRepository(private val bookDao: BookDao) {
    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()
    
    suspend fun getBookById(id: Int): BookEntity? = bookDao.getBookById(id)
    
    suspend fun insertBook(book: BookEntity) = bookDao.insertBook(book)
    
    suspend fun updateBook(book: BookEntity) = bookDao.updateBook(book)
    
    suspend fun deleteBook(book: BookEntity) = bookDao.deleteBook(book)
}
