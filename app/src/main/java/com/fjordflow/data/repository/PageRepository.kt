package com.fjordflow.data.repository

import com.fjordflow.data.db.dao.PageDao
import com.fjordflow.data.db.entity.PageEntity
import kotlinx.coroutines.flow.Flow

class PageRepository(private val pageDao: PageDao) {
    fun getPagesForBook(bookId: Int): Flow<List<PageEntity>> = pageDao.getPagesForBook(bookId)
    suspend fun getMaxPageNumber(bookId: Int): Int? = pageDao.getMaxPageNumber(bookId)
    suspend fun insertPage(page: PageEntity): Long = pageDao.insertPage(page)
    suspend fun deletePage(page: PageEntity) = pageDao.deletePage(page)
}
