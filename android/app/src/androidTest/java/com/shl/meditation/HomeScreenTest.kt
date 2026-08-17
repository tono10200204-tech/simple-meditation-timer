package com.shl.meditation

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shl.meditation.ui.home.HomeScreen
import com.shl.meditation.ui.home.HomeState
import com.shl.meditation.ui.theme.MeditationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The figure is the only control in the app, so a screen reader that cannot
 * name it and press it in one element cannot use the app at all. This asserts
 * against the merged semantics tree — the one a screen reader actually reads.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theFigureIsANamedButtonThatCanBePressed() {
        var tapped = false
        compose.setContent {
            MeditationTheme {
                HomeScreen(
                    state = HomeState.Idle(selectedMinutes = 30, totalSeconds = 0),
                    onFigureTap = { tapped = true },
                    onPresetSelected = {},
                    onRecord = {},
                    onDiscard = {},
                    onTotalHoursEntered = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Start meditation")
            .assertHasClickAction()
            .performClick()

        assertTrue("pressing the named element should start a session", tapped)
    }
}
