package com.example.audioplayer.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audioplayer.PlayerUiState

private data class TrackMetadata(val name: String, val art: ByteArray?)

@Composable
fun PlayerControls(
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableStateOf(0f) }

    val actualProgress = if (state.durationMs > 0) {
        state.currentPositionMs.toFloat() / state.durationMs.toFloat()
    } else 0f
    val displayProgress = if (isDragging) sliderValue else actualProgress

    val trackMetadata = remember(state.trackName, state.albumArt) {
        TrackMetadata(state.trackName, state.albumArt)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D0D))
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = trackMetadata,
            transitionSpec = { fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500)) },
            label = "track_transition"
        ) { metadata ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (metadata.art != null) {
                    AsyncImage(
                        model = metadata.art,
                        contentDescription = "Обложка трека",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1A1A1A))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1A1A1A)),
                        contentAlignment = Alignment.Center
                    ) { Text(text = "🎵", fontSize = 64.sp) }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = metadata.name, color = Color.White, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 16.dp).padding(horizontal = 8.dp)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatTime(if (isDragging) (sliderValue * state.durationMs).toLong() else state.currentPositionMs),
                color = Color.LightGray, fontSize = 12.sp
            )
            Slider(
                value = displayProgress,
                onValueChange = { isDragging = true; sliderValue = it },
                onValueChangeFinished = { isDragging = false; onSeek((sliderValue * state.durationMs).toLong()) },
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFE53935),
                    activeTrackColor = Color(0xFFE53935),
                    inactiveTrackColor = Color(0xFF2C2C2C)
                )
            )
            Text(text = formatTime(state.durationMs), color = Color.LightGray, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleShuffle) {
                Text(
                    text = "🔀",
                    color = if (state.isShuffleEnabled) Color(0xFFE53935) else Color.Gray,
                    fontSize = 20.sp
                )
            }

            IconButton(onClick = onPrev, modifier = Modifier.size(48.dp)) {
                Text(text = "⏮", fontSize = 24.sp, color = Color.White)
            }

            Button(
                onClick = onPlayPause,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                modifier = Modifier.size(72.dp),
                contentPadding = PaddingValues(0.dp)
            ) { Text(text = if (state.isPlaying) "⏸" else "▶", fontSize = 28.sp, color = Color.White) }

            IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                Text(text = "⏭", fontSize = 24.sp, color = Color.White)
            }

            IconButton(onClick = onToggleRepeat) {
                val (iconText, iconColor) = when (state.repeatMode) {
                    androidx.media3.common.Player.REPEAT_MODE_ONE -> "🔂" to Color(0xFFE53935)
                    androidx.media3.common.Player.REPEAT_MODE_ALL -> "🔁" to Color(0xFFE53935)
                    else -> "🔁" to Color.Gray
                }
                Text(text = iconText, color = iconColor, fontSize = 20.sp)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
