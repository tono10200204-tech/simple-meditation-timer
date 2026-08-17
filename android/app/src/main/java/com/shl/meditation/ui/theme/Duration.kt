package com.shl.meditation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.shl.meditation.R

/**
 * Whole hours, rounded up.
 *
 * A lifetime total is a number to watch grow, not to read to the minute, and
 * rounding down would leave a first session sitting at "0 h" as if it had not
 * happened. The dialog that edits the total seeds itself from here too, so the
 * number offered for editing is the one that was just tapped.
 */
fun totalHours(seconds: Int): Int = (seconds + 3599) / 3600

@Composable
fun totalHoursText(seconds: Int): String = stringResource(R.string.hours_only, totalHours(seconds))
