package dev.yas.ledger.ledger

import java.math.BigDecimal
import java.math.RoundingMode

const val RUPEE_SIGN = "₹"

private const val PAISE_PER_RUPEE = 100L
private val RUPEE_INPUT = Regex("""^\d+(\.\d{1,2})?$""")

/** Formats paise as rupees with Indian digit grouping. Paise are shown only when non-zero. */
fun formatRupees(paise: Long): String {
    val sign = if (paise < 0) "-" else ""
    val absolute = if (paise < 0) -paise else paise
    val rupees = (absolute / PAISE_PER_RUPEE).toString()
    val remainder = absolute % PAISE_PER_RUPEE
    val body = if (remainder == 0L) {
        groupIndian(rupees)
    } else {
        groupIndian(rupees) + "." + remainder.toString().padStart(2, '0')
    }
    return "$sign$RUPEE_SIGN$body"
}

/** Plain digits for an input field: no symbol, no grouping. */
fun formatPlainRupees(paise: Long): String {
    val rupees = paise / PAISE_PER_RUPEE
    val remainder = paise % PAISE_PER_RUPEE
    return if (remainder == 0L) rupees.toString() else "$rupees.${remainder.toString().padStart(2, '0')}"
}

/**
 * Reads what the user typed (commas allowed) into paise. Returns null for blank, zero,
 * negative, more than two decimals, or anything that is not a plain number.
 */
fun parseRupeesToPaise(input: String): Long? {
    val cleaned = input.trim().replace(",", "")
    if (!RUPEE_INPUT.matches(cleaned)) return null
    val paise = BigDecimal(cleaned).movePointRight(2).setScale(0, RoundingMode.HALF_UP)
    if (paise.signum() <= 0 || paise.bitLength() >= Long.SIZE_BITS - 1) return null
    return paise.toLong()
}

private fun groupIndian(digits: String): String {
    if (digits.length <= 3) return digits
    val lastThree = digits.takeLast(3)
    val rest = digits.dropLast(3)
    val groups = rest.reversed().chunked(2).joinToString(",").reversed()
    return "$groups,$lastThree"
}
