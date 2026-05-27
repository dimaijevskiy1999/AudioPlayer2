package com.example.audioplayer

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerUiState(
    val trackName: String  = "Нет активного трека",
    val isPlaying: Boolean = false,
    val audioFiles: List<AudioFile> = emptyList(),
    val error: String?     = null
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controller: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _uiState.value = _uiState.value.copy(
                trackName = mediaItem?.mediaMetadata?.title?.toString()
                    ?: mediaItem?.localConfiguration?.uri?.lastPathSegment
                    ?: "Неизвестный трек",
                error = null
            )
        }
    }

    init {
        val token = SessionToken(
            application,
            ComponentName(application, PlaybackService::class.java)
        )
        val future = MediaController.Builder(application, token).buildAsync()
        future.addListener({
            try {
                controller = future.get()
                controller?.addListener(playerListener)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Ошибка подключения к сервису")
            }
        }, MoreExecutors.directExecutor())

        loadAudioFiles()
    }

    private fun loadAudioFiles() {
        val files = getAudioFiles(getApplication())
        _uiState.value = _uiState.value.copy(audioFiles = files)
    }

    fun playTrack(file: AudioFile) {
        try {
            controller?.apply {
                setMediaItem(
                    MediaItem.Builder()
                        .setUri(file.uri)
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(file.name.removeSuffix(".mp3"))
                                .build()
                        )
                        .build()
                )
                prepare()
                play()
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Не удалось воспроизвести: ${file.name}")
        }
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun nextTrack() {
        controller?.seekToNextMediaItem()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        controller?.removeListener(playerListener)
        controller?.release()
        super.onCleared()
    }
}
