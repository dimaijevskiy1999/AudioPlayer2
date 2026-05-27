package com.example.audioplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audioplayer.ui.theme.AudioPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudioPlayerTheme {
                val viewModel: PlayerViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()

                PlayerScreen(
                    viewModel = viewModel,
                    uiState   = uiState
                )
            }
        }
    }
}
