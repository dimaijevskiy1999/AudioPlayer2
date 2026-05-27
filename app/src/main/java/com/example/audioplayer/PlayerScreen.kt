package com.example.audioplayer

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audioplayer.ui.components.PlayerControls
import com.example.audioplayer.ui.components.TrackList

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    uiState: PlayerUiState
) {
    var permissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        if (isGranted) {
            viewModel.loadAudioFiles()
        }
    }

    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(permission)
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted && uiState.audioFiles.isEmpty()) {
            viewModel.loadAudioFiles()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding() 
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1A1A1A)
        ) {
            Text(
                text = "🎵  AudioPlayer",
                color = Color(0xFFE53935),
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }

        if (!permissionGranted) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Нет доступа к аудиофайлам",
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = {
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                Manifest.permission.READ_MEDIA_AUDIO
                            else
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            permissionLauncher.launch(permission)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("Предоставить доступ")
                    }
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                TrackList(
                    files = uiState.audioFiles,
                    currentTrackName = uiState.trackName,
                    onTrackClick = { viewModel.playTrack(it) }
                )
            }
        }

        PlayerControls(
            state = uiState,
            onPlayPause = { viewModel.togglePlayPause() },
            onNext = { viewModel.nextTrack() },
            onPrev = { viewModel.previousTrack() },
            onSeek = { position -> viewModel.seekTo(position) },
            onToggleShuffle = { viewModel.toggleShuffle() },
            onToggleRepeat = { viewModel.toggleRepeat() }
        )

        if (uiState.error != null) {
            Snackbar(
                modifier = Modifier.padding(8.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK", color = Color.White)
                    }
                },
                containerColor = Color(0xFF8B0000)
            ) {
                Text(uiState.error ?: "", color = Color.White)
            }
        }
    }
}
