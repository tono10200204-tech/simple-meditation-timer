package com.shl.meditation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shl.meditation.ui.home.HomeScreen
import com.shl.meditation.ui.home.HomeViewModel
import com.shl.meditation.ui.theme.MeditationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeditationTheme {
                val viewModel: HomeViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    HomeScreen(
                        state = state,
                        onFigureTap = viewModel::onFigureTap,
                        onPresetSelected = viewModel::onPresetSelected,
                        onRecord = viewModel::onRecord,
                        onDiscard = viewModel::onDiscard,
                        onTotalHoursEntered = viewModel::onTotalHoursEntered,
                    )
                }
            }
        }
    }
}
