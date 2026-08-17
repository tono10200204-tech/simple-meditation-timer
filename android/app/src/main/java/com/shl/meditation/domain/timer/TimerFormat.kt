package com.shl.meditation.domain.timer

import java.util.Locale
import kotlin.math.absoluteValue

/**
 * "29:42" while counting down, "+03:12" once the planned time has passed.
 *
 * Minutes are always two digits so the string never changes length. Together
 * with the tabular figures in the type scale, that keeps the display from
 * shifting sideways as the seconds change. Minutes are not rolled into hours:
 * a 90 minute sit reads "90:00", which is the number the user chose.
 */
fun formatTimerDisplay(remainingMillis: Long): String {
    val overtime = remainingMillis < 0

    // Counting down, round up so a fresh 30 minute session reads "30:00" and
    // "00:00" appears only at the moment the bell is due. Counting up, round
    // down so the first second past zero reads "+00:00".
    val totalSeconds = if (overtime) {
        remainingMillis.absoluteValue / 1000
    } else {
        (remainingMillis + 999) / 1000
    }

    // Locale.ROOT keeps the digits Western. A locale with its own numerals
    // would render glyphs of differing width and undo the tabular figures.
    val text = String.format(Locale.ROOT, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    return if (overtime) "+$text" else text
}
