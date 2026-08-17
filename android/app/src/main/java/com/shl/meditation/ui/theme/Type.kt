package com.shl.meditation.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The remaining time is redrawn every second, so the digits must not change
 * width as they change value. "tnum" selects tabular figures, which keeps every
 * digit the same advance width and stops the display from jittering.
 */
val TimerNumerals = TextStyle(
    fontSize = 76.sp,
    fontWeight = FontWeight.Light,
    letterSpacing = (-1.5).sp,
    fontFeatureSettings = "tnum",
)

/** The Pali phrase under the ring. Quiet, but large enough to read at arm's length. */
val MettaPhrase = TextStyle(
    fontSize = 14.sp,
    letterSpacing = 0.4.sp,
)

/** 15 / 30 / 60 / 120. Selection is shown by weight of colour, not by size. */
val PresetLabel = TextStyle(
    fontSize = 19.sp,
    fontFeatureSettings = "tnum",
)

