package com.fjordflow.data.repository

import com.fjordflow.data.db.dao.FlashCardDao
import com.fjordflow.data.db.entity.FlashCardEntity
import kotlinx.coroutines.flow.Flow
import kotlin.math.max
import kotlin.math.roundToInt

enum class ReviewQuality { AGAIN, HARD, GOOD, EASY }

class FlashCardRepository(private val dao: FlashCardDao) {

    fun getDueCards(): Flow<List<FlashCardEntity>> =
        dao.getDueCards(System.currentTimeMillis())

    fun getAllCards(): Flow<List<FlashCardEntity>> = dao.getAllCards()
    fun getTotalCount(): Flow<Int> = dao.getTotalCount()
    fun getDueCount(): Flow<Int> = dao.getDueCount(System.currentTimeMillis())

    suspend fun reviewCard(card: FlashCardEntity, quality: ReviewQuality) {
        dao.updateCard(applySmTwo(card, quality))
    }

    /**
     * Simplified SM-2 algorithm.
     * AGAIN → reset to 1 day; HARD → 1.2x; GOOD → easeFactor * interval; EASY → 1.3 * easeFactor * interval
     */
    private fun applySmTwo(card: FlashCardEntity, quality: ReviewQuality): FlashCardEntity {
        val newEase = when (quality) {
            ReviewQuality.AGAIN -> max(1.3f, card.easeFactor - 0.2f)
            ReviewQuality.HARD  -> max(1.3f, card.easeFactor - 0.15f)
            ReviewQuality.GOOD  -> card.easeFactor
            ReviewQuality.EASY  -> card.easeFactor + 0.1f
        }
        val newInterval = when (quality) {
            ReviewQuality.AGAIN -> 1
            ReviewQuality.HARD  -> max(1, (card.intervalDays * 1.2f).roundToInt())
            ReviewQuality.GOOD  -> when (card.reviewCount) {
                0 -> 1
                1 -> 6
                else -> (card.intervalDays * card.easeFactor).roundToInt()
            }
            ReviewQuality.EASY  -> when (card.reviewCount) {
                0 -> 4
                else -> (card.intervalDays * card.easeFactor * 1.3f).roundToInt()
            }
        }
        val dueDate = System.currentTimeMillis() + newInterval.toLong() * 86_400_000L
        return card.copy(
            easeFactor = newEase,
            intervalDays = newInterval,
            dueDate = dueDate,
            reviewCount = card.reviewCount + 1
        )
    }
}
