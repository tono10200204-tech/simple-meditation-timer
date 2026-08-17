package com.shl.meditation.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.PowerManager
import com.shl.meditation.R

/** No session runs this long; the cap only stops a bug from draining a battery. */
private const val WAKE_LOCK_LIMIT_MILLIS = 4 * 60 * 60 * 1000L

/**
 * Plays the bell once and releases itself.
 *
 * The alarm stream is the right one: the user asked for this sound by starting
 * a timer, so a silenced phone should still ring.
 */
fun ringBell(context: Context) {
    val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    val sessionId = context.getSystemService(AudioManager::class.java).generateAudioSessionId()

    MediaPlayer.create(context, R.raw.bell, attributes, sessionId)?.apply {
        setOnCompletionListener { it.release() }
        // Without this an error would leave the player holding a codec forever,
        // since onCompletion never arrives for a player that failed.
        setOnErrorListener { player, _, _ ->
            player.release()
            true
        }
        start()
    }
}

/**
 * Keeps the CPU awake while a session is open.
 *
 * The phone is meant to be face down with the screen off, and Android would
 * otherwise suspend the process long before a thirty minute sit is over. This
 * is deliberately not an alarm: closing the app ends the meditation, so there
 * is nothing left to ring for.
 */
class SessionWakeLock(context: Context) {

    private val lock = context.getSystemService(PowerManager::class.java)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "meditation:session")

    fun hold() {
        if (!lock.isHeld) lock.acquire(WAKE_LOCK_LIMIT_MILLIS)
    }

    fun release() {
        if (lock.isHeld) lock.release()
    }
}
