package com.example.audioplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audioplayer.PlayerUiState

@Composable
fun PlayerControls(
    state: PlayerUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D0D))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Название трека
        Text(
            text = state.trackName,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        // Кнопки управления
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Pause
            Button(
                onClick = onPlayPause,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935)
                ),
                modifier = Modifier.size(64.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (state.isPlaying) "⏸" else "▶",
                    fontSize = 22.sp,
                    color = Color.White
                )
            }

            // Next
            Button(
                onClick = onNext,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C2C2C)
                ),
                modifier = Modifier.size(50.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "⏭",
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }
    }
}
