package com.shl.meditation.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shl.meditation.R
import com.shl.meditation.ui.theme.MettaPhrase
import com.shl.meditation.ui.theme.PresetLabel
import com.shl.meditation.ui.theme.TimerNumerals
import com.shl.meditation.ui.theme.totalHours
import com.shl.meditation.ui.theme.totalHoursText

private val PRESET_MINUTES = listOf(15, 30, 60, 120)

/** Everything below the figure lives in a fixed-height area so that the figure
 *  itself never moves between states — the target stays exactly where the
 *  user's thumb left it. */
private val BELOW_FIGURE_HEIGHT = 220.dp

@Composable
fun HomeScreen(
    state: HomeState,
    onFigureTap: () -> Unit,
    onPresetSelected: (Int) -> Unit,
    onRecord: () -> Unit,
    onDiscard: () -> Unit,
    onTotalHoursEntered: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingTotal by remember { mutableStateOf(false) }

    // While meditating, back should not throw the session away by accident.
    BackHandler(enabled = state !is HomeState.Idle) {}

    // enableEdgeToEdge draws behind the system bars, so anything pinned to an
    // edge has to be inset or it ends up under the clock.
    Box(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Figure(state = state, onTap = onFigureTap)

            Box(
                modifier = Modifier
                    .height(BELOW_FIGURE_HEIGHT)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                when (state) {
                    is HomeState.Idle -> IdleControls(state.selectedMinutes, onPresetSelected)
                    is HomeState.Running -> TimeDisplay(state.display, muted = false)
                    is HomeState.Paused -> PausedControls(
                        state = state,
                        onRecord = onRecord,
                        onDiscard = onDiscard,
                    )
                }
            }
        }

        // Idle only: during a sit there is nothing on screen but the time.
        if (state is HomeState.Idle) {
            // Tapping the total is how it gets corrected — there is no backup,
            // so being able to type the number back in is the recovery story.
            Text(
                text = totalHoursText(state.totalSeconds),
                style = MettaPhrase,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
                    .quietlyClickable { editingTotal = true }
                    .padding(horizontal = 32.dp, vertical = 12.dp),
            )
        }
    }

    if (editingTotal) {
        EditTotalDialog(
            currentSeconds = (state as? HomeState.Idle)?.totalSeconds ?: 0,
            onSave = onTotalHoursEntered,
            onDismiss = { editingTotal = false },
        )
    }

}

/** On a screen this quiet a ripple would be the loudest thing on it. */
@Composable
private fun Modifier.quietlyClickable(onClick: () -> Unit): Modifier = clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick,
)

@Composable
private fun Figure(state: HomeState, onTap: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val label = stringResource(
        when (state) {
            is HomeState.Idle -> R.string.start_meditation
            is HomeState.Running -> R.string.pause_meditation
            is HomeState.Paused -> R.string.resume_meditation
        },
    )

    Box(
        modifier = Modifier
            .size(248.dp)
            .clip(CircleShape)
            .quietlyClickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onTap()
            }
            // The name has to sit on the thing that can be pressed. Left on the
            // image inside, a screen reader announces one element and activates
            // another — which in an app with a single control means no control.
            .semantics {
                contentDescription = label
                role = Role.Button
            }
            .border(0.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_meditation),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                if (state is HomeState.Paused) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
            ),
            modifier = Modifier.size(150.dp),
        )
    }
}

@Composable
private fun IdleControls(selectedMinutes: Int, onPresetSelected: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.metta_phrase),
            style = MettaPhrase,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(58.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            PRESET_MINUTES.forEach { minutes ->
                val selected = minutes == selectedMinutes
                Text(
                    text = minutes.toString(),
                    style = PresetLabel,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onPresetSelected(minutes) },
                )
            }
        }
    }
}

@Composable
private fun PausedControls(state: HomeState.Paused, onRecord: () -> Unit, onDiscard: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TimeDisplay(state.display, muted = true)

        Spacer(Modifier.height(30.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            QuietAction(stringResource(R.string.record), emphasised = true, onClick = onRecord)
            QuietAction(
                text = stringResource(if (state.pastBell) R.string.done else R.string.discard),
                emphasised = false,
                onClick = onDiscard,
            )
        }
    }
}

/**
 * A Column of its own, not just a Spacer next to a Text — a Box only overlays
 * children rather than stacking them, so called directly inside one (as the
 * Running case does) a bare Spacer sibling would be measured but ignored, and
 * the number would sit 24dp higher than it does here in the Paused case,
 * where an outer Column happens to stack them correctly.
 */
@Composable
private fun TimeDisplay(display: String, muted: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = display,
            style = TimerNumerals,
            color = if (muted) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onBackground
            },
        )
    }
}

@Composable
private fun QuietAction(text: String, emphasised: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            style = MettaPhrase,
            color = if (emphasised) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun EditTotalDialog(currentSeconds: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var hours by remember { mutableStateOf(totalHours(currentSeconds).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_total_title)) },
        text = {
            OutlinedTextField(
                value = hours,
                onValueChange = { hours = it.filter(Char::isDigit).take(5) },
                suffix = { Text(stringResource(R.string.edit_total_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(hours.toIntOrNull() ?: 0)
                    onDismiss()
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        containerColor = MaterialTheme.colorScheme.background,
    )
}

