package com.fjordflow.data.repository

import com.fjordflow.data.db.dao.FlashCardDao
import com.fjordflow.data.db.dao.WordDao
import com.fjordflow.data.db.entity.FlashCardEntity
import com.fjordflow.data.db.entity.WordEntity
import kotlinx.coroutines.flow.Flow

class WordRepository(
    private val wordDao: WordDao,
    private val flashCardDao: FlashCardDao
) {
    fun getAllWords(): Flow<List<WordEntity>> = wordDao.getAllWords()

    suspend fun saveWordAndCreateCard(
        word: String,
        context: String,
        translation: String,
        frontOverride: String? = null
    ) {
        val entity = WordEntity(
            word = word,
            context = context,
            translation = translation
        )
        val wordId = wordDao.insertWord(entity).toInt()
        flashCardDao.insertCard(
            FlashCardEntity(
                wordId = wordId,
                front = frontOverride ?: word,
                back = translation
            )
        )
    }

    suspend fun deleteWord(word: WordEntity) = wordDao.deleteWord(word)
}
