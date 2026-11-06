package dev.yas.ledger.ledger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun `groups digits the Indian way`() {
        assertEquals("₹0", formatRupees(0))
        assertEquals("₹999", formatRupees(99_900))
        assertEquals("₹1,000", formatRupees(100_000))
        assertEquals("₹12,345.67", formatRupees(1_234_567))
        assertEquals("₹1,23,456", formatRupees(12_345_600))
        assertEquals("₹12,34,567", formatRupees(123_456_700))
    }

    @Test
    fun `shows paise only when non-zero and pads them`() {
        assertEquals("₹5.05", formatRupees(505))
        assertEquals("₹5", formatRupees(500))
    }

    @Test
    fun `negative amounts keep the sign before the symbol`() {
        assertEquals("-₹150", formatRupees(-15_000))
    }

    @Test
    fun `plain rupees have no symbol or grouping`() {
        assertEquals("1500", formatPlainRupees(150_000))
        assertEquals("1500.50", formatPlainRupees(150_050))
    }

    @Test
    fun `parses typed amounts into paise`() {
        assertEquals(150_000L, parseRupeesToPaise("1500"))
        assertEquals(150_000L, parseRupeesToPaise("1,500"))
        assertEquals(150_050L, parseRupeesToPaise("1500.5"))
        assertEquals(150_025L, parseRupeesToPaise("1500.25"))
        assertEquals(50L, parseRupeesToPaise("0.50"))
    }

    @Test
    fun `rejects blank, zero, negative, extra decimals and junk`() {
        assertNull(parseRupeesToPaise(""))
        assertNull(parseRupeesToPaise("   "))
        assertNull(parseRupeesToPaise("0"))
        assertNull(parseRupeesToPaise("0.00"))
        assertNull(parseRupeesToPaise("-5"))
        assertNull(parseRupeesToPaise("1.234"))
        assertNull(parseRupeesToPaise("abc"))
        assertNull(parseRupeesToPaise("1.2.3"))
        assertNull(parseRupeesToPaise("99999999999999999999"))
    }
}
