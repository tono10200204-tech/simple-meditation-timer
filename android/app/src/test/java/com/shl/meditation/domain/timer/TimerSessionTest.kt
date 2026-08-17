package com.shl.meditation.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MINUTE = 60_000L
private const val START = 1_000_000L // an arbitrary clock reading, not zero

/** Most cases are about the countdown, so they opt out of the settling time. */
private fun session(minutes: Long, now: Long = START) =
    startSession(minutes * MINUTE, now, graceMillis = 0)

class TimerSessionTest {

    @Test
    fun `a fresh session has its full duration remaining`() {
        val session = session(30)

        assertEquals(30 * MINUTE, session.remainingMillis(START))
        assertEquals(0, session.elapsedMillis(START))
    }

    @Test
    fun `time passing reduces the remaining time`() {
        val session = session(30)

        assertEquals(29 * MINUTE, session.remainingMillis(START + MINUTE))
        assertEquals(MINUTE, session.elapsedMillis(START + MINUTE))
    }

    @Test
    fun `a paused session stops counting down`() {
        val session = session(30).pause(START + 5 * MINUTE)

        // Ten minutes of wall time pass, but the session stays at five.
        assertEquals(25 * MINUTE, session.remainingMillis(START + 15 * MINUTE))
        assertTrue(session.isPaused)
    }

    @Test
    fun `resuming continues from where it paused`() {
        val session = session(30)
            .pause(START + 5 * MINUTE)
            .resume(START + 15 * MINUTE) // ten minutes spent paused

        assertFalse(session.isPaused)
        assertEquals(5 * MINUTE, session.elapsedMillis(START + 15 * MINUTE))
        assertEquals(6 * MINUTE, session.elapsedMillis(START + 16 * MINUTE))
    }

    @Test
    fun `pause time accumulates across several pauses`() {
        val session = session(30)
            .pause(START + 2 * MINUTE)
            .resume(START + 4 * MINUTE)
            .pause(START + 6 * MINUTE)
            .resume(START + 10 * MINUTE)

        // Ten minutes of wall time, six of them paused.
        assertEquals(4 * MINUTE, session.elapsedMillis(START + 10 * MINUTE))
        assertEquals(26 * MINUTE, session.remainingMillis(START + 10 * MINUTE))
    }

    @Test
    fun `pausing twice does not restart the pause`() {
        val session = session(30)
            .pause(START + 5 * MINUTE)
            .pause(START + 9 * MINUTE)

        assertEquals(5 * MINUTE, session.elapsedMillis(START + 9 * MINUTE))
    }

    @Test
    fun `resuming a running session changes nothing`() {
        val session = session(30)

        assertEquals(session, session.resume(START + MINUTE))
    }

    @Test
    fun `the timer keeps counting past zero`() {
        val session = session(30)

        assertTrue(session.hasReachedZero(START + 30 * MINUTE))
        assertEquals(0, session.remainingMillis(START + 30 * MINUTE))
        assertEquals(-3 * MINUTE, session.remainingMillis(START + 33 * MINUTE))
        assertEquals(33 * MINUTE, session.elapsedMillis(START + 33 * MINUTE))
    }

    @Test
    fun `overtime can be paused like any other part of the session`() {
        val session = session(30)
            .pause(START + 33 * MINUTE)

        assertEquals(33 * MINUTE, session.elapsedMillis(START + 40 * MINUTE))
    }

    @Test
    fun `the countdown includes the settling time`() {
        val session = startSession(15 * MINUTE, START)

        assertEquals(15 * MINUTE + 10_000, session.remainingMillis(START))
        assertTrue(session.isSettling(START))
    }

    @Test
    fun `settling ends when the grace is up and the opening bell is due`() {
        val session = startSession(15 * MINUTE, START)

        assertTrue(session.isSettling(START + 9_999))
        assertFalse(session.isSettling(START + 10_000))
    }

    @Test
    fun `settling time is not recorded`() {
        val session = startSession(15 * MINUTE, START)

        assertEquals(0, session.recordedMillis(START + 5_000))
        assertEquals(0, session.recordedMillis(START + 10_000))
        assertEquals(MINUTE, session.recordedMillis(START + 10_000 + MINUTE))
    }

    @Test
    fun `a full sit records the time that was asked for`() {
        val session = startSession(15 * MINUTE, START)
        val bell = START + 15 * MINUTE + 10_000

        assertEquals(0, session.remainingMillis(bell))
        assertEquals(15 * MINUTE, session.recordedMillis(bell))
    }

    @Test
    fun `sitting on past the bell records the extra time`() {
        val session = startSession(15 * MINUTE, START)

        assertEquals(18 * MINUTE, session.recordedMillis(START + 18 * MINUTE + 10_000))
    }
}
