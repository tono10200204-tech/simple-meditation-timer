package com.shl.meditation.domain.timer

/** Time to put the phone down and settle before the meditation is counted. */
const val SETTLING_GRACE_MILLIS = 10_000L

/**
 * A meditation in progress.
 *
 * The timer never counts ticks. Elapsed and remaining time are always derived
 * from these numbers and a fresh clock reading, which is what makes the timer
 * survive the screen going off: persist nothing, read the clock again, and the
 * answer is still right.
 *
 * All times are milliseconds from [android.os.SystemClock.elapsedRealtime],
 * which counts steadily while the device sleeps and — unlike wall-clock time —
 * cannot be moved by the user or by an NTP correction.
 *
 * Nothing here touches Android, so every case below is tested by passing a
 * number for `now` rather than by waiting.
 */
data class TimerSession(
    /** The whole countdown, settling time included. */
    val plannedMillis: Long,
    val startedAt: Long,
    /** Leading seconds that do not count as meditation. */
    val graceMillis: Long = SETTLING_GRACE_MILLIS,
    /** Set while paused, null while running. */
    val pausedAt: Long? = null,
    /** Total time already spent paused, excluded from the session. */
    val pausedTotalMillis: Long = 0,
) {
    val isPaused: Boolean get() = pausedAt != null

    /** Time since the tap, pauses excluded. */
    fun elapsedMillis(now: Long): Long = (pausedAt ?: now) - startedAt - pausedTotalMillis

    /** Negative once the planned time has passed — the timer keeps going. */
    fun remainingMillis(now: Long): Long = plannedMillis - elapsedMillis(now)

    fun hasReachedZero(now: Long): Boolean = remainingMillis(now) <= 0

    /** True until the settling time is up and the opening bell is due. */
    fun isSettling(now: Long): Boolean = elapsedMillis(now) < graceMillis

    /** What goes in the record: the settling time is not meditation. */
    fun recordedMillis(now: Long): Long =
        (elapsedMillis(now) - graceMillis).coerceAtLeast(0)

    fun pause(now: Long): TimerSession = if (isPaused) this else copy(pausedAt = now)

    fun resume(now: Long): TimerSession = when (pausedAt) {
        null -> this
        else -> copy(pausedAt = null, pausedTotalMillis = pausedTotalMillis + (now - pausedAt))
    }
}

/** [meditationMillis] is the time asked for; the settling time is added on top. */
fun startSession(
    meditationMillis: Long,
    now: Long,
    graceMillis: Long = SETTLING_GRACE_MILLIS,
) = TimerSession(
    plannedMillis = meditationMillis + graceMillis,
    startedAt = now,
    graceMillis = graceMillis,
)
