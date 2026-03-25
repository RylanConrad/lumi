package com.fjordflow.data.repository

import com.fjordflow.data.db.dao.FlashCardDao
import com.fjordflow.data.db.entity.FlashCardEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlin.math.max
import kotlin.math.roundToInt

enum class ReviewQuality { AGAIN, HARD, GOOD, EASY }

class FlashCardRepository(private val dao: FlashCardDao) {

    /**
     * Emits the current timestamp every 30 seconds to refresh "due" status
     */
    private val timerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getDueCards(): Flow<List<FlashCardEntity>> =
        timerFlow.flatMapLatest { now -> dao.getDueCards(now) }

    fun getAllCards(): Flow<List<FlashCardEntity>> = dao.getAllCards()
    fun getTotalCount(): Flow<Int> = dao.getTotalCount()
    
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getDueCount(): Flow<Int> = 
        timerFlow.flatMapLatest { now -> dao.getDueCount(now) }

    suspend fun reviewCard(card: FlashCardEntity, quality: ReviewQuality) {
        dao.updateCard(applySmTwo(card, quality))
    }

    private fun applySmTwo(card: FlashCardEntity, quality: ReviewQuality): FlashCardEntity {
        val newEase = when (quality) {
            ReviewQuality.AGAIN -> max(1.3f, card.easeFactor - 0.2f)
            ReviewQuality.HARD  -> max(1.3f, card.easeFactor - 0.15f)
            ReviewQuality.GOOD  -> card.easeFactor
            ReviewQuality.EASY  -> card.easeFactor + 0.15f
        }

        val newIntervalDays: Int
        val dueDateMs: Long

        when (quality) {
            ReviewQuality.AGAIN -> {
                newIntervalDays = 1
                dueDateMs = System.currentTimeMillis() + 60_000L 
            }
            ReviewQuality.HARD -> {
                newIntervalDays = max(1, (card.intervalDays * 1.2f).roundToInt())
                dueDateMs = System.currentTimeMillis() + newIntervalDays.toLong() * 86_400_000L
            }
            ReviewQuality.GOOD -> {
                newIntervalDays = when (card.reviewCount) {
                    0 -> 1
                    1 -> 6
                    else -> (card.intervalDays * card.easeFactor).roundToInt()
                }
                dueDateMs = System.currentTimeMillis() + newIntervalDays.toLong() * 86_400_000L
            }
            ReviewQuality.EASY -> {
                newIntervalDays = when (card.reviewCount) {
                    0 -> 4
                    else -> (card.intervalDays * card.easeFactor * 1.3f).roundToInt()
                }
                dueDateMs = System.currentTimeMillis() + newIntervalDays.toLong() * 86_400_000L
            }
        }

        return card.copy(
            easeFactor = newEase,
            intervalDays = newIntervalDays,
            dueDate = dueDateMs,
            reviewCount = card.reviewCount + 1
        )
    }
}
