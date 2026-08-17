package com.shl.meditation.ui.home

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shl.meditation.data.MeditationDatabase
import com.shl.meditation.data.MeditationSession
import com.shl.meditation.data.Preferences
import com.shl.meditation.domain.timer.TimerSession
import com.shl.meditation.domain.timer.formatTimerDisplay
import com.shl.meditation.domain.timer.startSession
import com.shl.meditation.platform.SessionWakeLock
import com.shl.meditation.platform.ringBell
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val DEFAULT_MINUTES = 30
private const val TICK_MILLIS = 200L

sealed interface HomeState {
    data class Idle(val selectedMinutes: Int, val totalSeconds: Int) : HomeState

    data class Running(val display: String) : HomeState

    /** [pastBell] decides whether the second action discards or simply finishes. */
    data class Paused(val display: String, val pastBell: Boolean) : HomeState
}

/**
 * A meditation lasts as long as the app is open. Closing it ends the session,
 * because someone who has moved on to something else is no longer meditating —
 * so there is no alarm to schedule and no session to persist.
 *
 * Finished sessions are a different matter: those go to Room and stay.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Everything belonging to one sitting.
     *
     * It is replaced wholesale when a session begins and dropped when one ends,
     * and a save holds its own reference, so a write still in flight can never
     * land on the next session's row.
     */
    private class Sit(val startedAtWallClock: Long) {
        var openingBellRung = false
        var closingBellRung = false
        var rowId: Long? = null
    }

    private val sessions = MeditationDatabase.get(application).sessions()
    private val preferences = Preferences(application)
    private val wakeLock = SessionWakeLock(application)

    private val session = MutableStateFlow<TimerSession?>(null)
    private val selectedMinutes = MutableStateFlow(DEFAULT_MINUTES)
    private var sit: Sit? = null

    /** Saves run one at a time, so an insert always finishes before an update goes looking for its id. */
    private val saving = Mutex()

    /** Redrawing is driven from here rather than from a countdown. */
    private val tick = MutableStateFlow(now())
    private var ticker: Job? = null

    private val recordedSeconds = sessions.observeTotalSeconds()

    /** What the home screen shows: everything sat here, on top of whatever was carried in. */
    private val totalSeconds =
        combine(recordedSeconds, preferences.importedTotalSeconds) { recorded, imported ->
            recorded + imported
        }

    val state: StateFlow<HomeState> =
        combine(session, selectedMinutes, tick, totalSeconds) { sitting, minutes, reading, total ->
            when {
                sitting == null -> HomeState.Idle(minutes, total)
                sitting.isPaused -> HomeState.Paused(
                    display = formatTimerDisplay(sitting.remainingMillis(reading)),
                    pastBell = sitting.hasReachedZero(reading),
                )
                else -> HomeState.Running(formatTimerDisplay(sitting.remainingMillis(reading)))
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeState.Idle(DEFAULT_MINUTES, totalSeconds = 0),
        )

    /** The figure is the only control: start, then pause, then resume. */
    fun onFigureTap() {
        val current = session.value
        val next = when {
            current == null -> beginSession()
            current.isPaused -> current.resume(now())
            else -> current.pause(now())
        }
        session.value = next
        if (next.isPaused) stopTicking() else startTicking()
    }

    fun onPresetSelected(minutes: Int) {
        selectedMinutes.value = minutes
    }

    /**
     * The user types the total they want to see, not an offset, so the carried-in
     * part is whatever is left once this app's own sessions are taken off. A
     * figure below what has already been sat here clamps to zero rather than
     * going negative: recorded sittings are not something to type away.
     */
    fun onTotalHoursEntered(hours: Int) {
        viewModelScope.launch {
            preferences.setImportedTotalSeconds(hours * 3600 - recordedSeconds.first())
        }
    }

    /** Saves what has been sat so far, then returns home. */
    fun onRecord() {
        save()
        finish()
    }

    /**
     * Before the bell this throws the session away. After it, the session was
     * already saved when the bell rang, so this only drops the extra time.
     */
    fun onDiscard() {
        finish()
    }

    override fun onCleared() {
        super.onCleared()
        wakeLock.release()
    }

    /**
     * The countdown starts a few seconds before the meditation does, which is
     * time to put the phone down. The opening bell marks the end of that, so it
     * is not rung here — nothing has begun yet.
     */
    private fun beginSession(): TimerSession {
        sit = Sit(startedAtWallClock = System.currentTimeMillis())
        return startSession(selectedMinutes.value * 60_000L, now())
    }

    private fun finish() {
        stopTicking()
        session.value = null
        sit = null
    }

    /**
     * The first save inserts and remembers the row; later ones update it, so
     * sitting on past the bell grows the record rather than adding a second.
     */
    private fun save() {
        val current = session.value ?: return
        val sit = this.sit ?: return
        val seconds = (current.recordedMillis(now()) / 1000).toInt()
        if (seconds <= 0) return

        viewModelScope.launch {
            saving.withLock {
                val row = MeditationSession(
                    id = sit.rowId ?: 0,
                    startedAt = sit.startedAtWallClock,
                    durationSeconds = seconds,
                )
                if (sit.rowId == null) sit.rowId = sessions.insert(row) else sessions.update(row)
            }
        }
    }

    private fun startTicking() {
        wakeLock.hold()
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                val now = now()
                tick.value = now
                ringBellsIfDue(now)
                delay(TICK_MILLIS)
            }
        }
    }

    private fun stopTicking() {
        ticker?.cancel()
        ticker = null
        wakeLock.release()
        tick.value = now()
    }

    /**
     * A bell opens the sitting as well as closing it, the way it is done in a
     * hall. The closing one also saves, right then rather than when the user
     * gets around to pressing something: falling asleep afterwards still leaves
     * the meditation recorded.
     */
    private fun ringBellsIfDue(now: Long) {
        val current = session.value ?: return
        val sit = this.sit ?: return

        if (!sit.openingBellRung && !current.isSettling(now)) {
            sit.openingBellRung = true
            ringBell(getApplication())
        }

        if (!sit.closingBellRung && current.hasReachedZero(now)) {
            sit.closingBellRung = true
            ringBell(getApplication())
            save()
        }
    }

    private fun now() = SystemClock.elapsedRealtime()
}
