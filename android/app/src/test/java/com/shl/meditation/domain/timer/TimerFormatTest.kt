package com.shl.meditation.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerFormatTest {

    @Test
    fun `a fresh session shows its full duration`() {
        assertEquals("30:00", formatTimerDisplay(30 * 60_000L))
    }

    @Test
    fun `part seconds round up while counting down`() {
        // 29:41.2 still has 42 seconds showing, exactly as a wall clock would.
        assertEquals("29:42", formatTimerDisplay(29 * 60_000L + 41_200))
    }

    @Test
    fun `zero is reached only when the time is actually up`() {
        assertEquals("00:01", formatTimerDisplay(1))
        assertEquals("00:00", formatTimerDisplay(0))
    }

    @Test
    fun `overtime is marked with a plus and counts up`() {
        assertEquals("+00:00", formatTimerDisplay(-500))
        assertEquals("+00:01", formatTimerDisplay(-1_000))
        assertEquals("+03:12", formatTimerDisplay(-(3 * 60_000L + 12_000)))
    }

    @Test
    fun `minutes are not rolled into hours`() {
        assertEquals("90:00", formatTimerDisplay(90 * 60_000L))
        assertEquals("120:00", formatTimerDisplay(120 * 60_000L))
    }

    @Test
    fun `the string keeps a stable width below ten minutes`() {
        assertEquals("09:05", formatTimerDisplay(9 * 60_000L + 5_000))
    }
}
