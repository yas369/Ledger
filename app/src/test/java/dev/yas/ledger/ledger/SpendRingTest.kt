package dev.yas.ledger.ledger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendRingTest {
    @Test
    fun `no limit means no remaining and an empty ring`() {
        val ring = SpendRing(spentPaise = 50_000, limitPaise = null)
        assertFalse(ring.hasLimit)
        assertNull(ring.remainingPaise)
        assertEquals(0f, ring.fraction, 0f)
        assertEquals(SpendLevel.OK, ring.level)
    }

    @Test
    fun `a zero limit is treated as not set`() {
        val ring = SpendRing(spentPaise = 50_000, limitPaise = 0)
        assertFalse(ring.hasLimit)
        assertNull(ring.remainingPaise)
    }

    @Test
    fun `below eighty percent is ok`() {
        val ring = SpendRing(spentPaise = 7_900, limitPaise = 10_000)
        assertEquals(SpendLevel.OK, ring.level)
        assertEquals(2_100L, ring.remainingPaise)
        assertEquals(0.79f, ring.fraction, 0.0001f)
    }

    @Test
    fun `exactly eighty percent is near`() {
        assertEquals(SpendLevel.NEAR, SpendRing(8_000, 10_000).level)
    }

    @Test
    fun `reaching the limit is over and remaining hits zero`() {
        val ring = SpendRing(10_000, 10_000)
        assertEquals(SpendLevel.OVER, ring.level)
        assertEquals(0L, ring.remainingPaise)
        assertTrue(ring.fraction == 1f)
    }

    @Test
    fun `overspending caps the fraction at one and remaining goes negative`() {
        val ring = SpendRing(15_000, 10_000)
        assertEquals(1f, ring.fraction, 0f)
        assertEquals(-5_000L, ring.remainingPaise)
        assertEquals(SpendLevel.OVER, ring.level)
    }

    @Test
    fun `nothing spent is an empty ok ring`() {
        val ring = SpendRing(0, 10_000)
        assertEquals(0f, ring.fraction, 0f)
        assertEquals(SpendLevel.OK, ring.level)
    }
}
