package dev.yas.ledger.ledger

enum class SpendLevel { OK, NEAR, OVER }

/**
 * Today's spending against the daily limit. All comparisons use whole paise so the 80% and
 * 100% boundaries are exact.
 */
data class SpendRing(val spentPaise: Long, val limitPaise: Long?) {
    private val limit: Long? get() = limitPaise?.takeIf { it > 0 }

    val hasLimit: Boolean get() = limit != null

    /** Share of the limit already spent, 0..1, for drawing the arc. */
    val fraction: Float
        get() {
            val l = limit ?: return 0f
            return (spentPaise.toDouble() / l).coerceIn(0.0, 1.0).toFloat()
        }

    val level: SpendLevel
        get() {
            val l = limit ?: return SpendLevel.OK
            return when {
                spentPaise >= l -> SpendLevel.OVER
                spentPaise * 5 >= l * 4 -> SpendLevel.NEAR
                else -> SpendLevel.OK
            }
        }

    /** Positive while under the limit, negative once over it; null when no limit is set. */
    val remainingPaise: Long? get() = limit?.let { it - spentPaise }
}
