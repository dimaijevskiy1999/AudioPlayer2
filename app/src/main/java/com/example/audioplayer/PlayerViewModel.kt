package com.example.audioplayer

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "player_prefs")

data class PlayerUiState(
    val trackName: String  = "Нет активного трека",
    val isPlaying: Boolean = false,
    val audioFiles: List<AudioFile> = emptyList(),
    val error: String?     = null,
    val lastPlayedUri: String? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val albumArt: ByteArray? = null,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controller: MediaController? = null
    private var progressJob: Job? = null

    private val LAST_TRACK_URI_KEY = stringPreferencesKey("last_track_uri")
    private val LAST_TRACK_NAME_KEY = stringPreferencesKey("last_track_name")

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
        }
        
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val title = mediaItem?.mediaMetadata?.title?.toString()
                ?: mediaItem?.localConfiguration?.uri?.lastPathSegment
                ?: "Неизвестный трек"

            _uiState.value = _uiState.value.copy(
                trackName = title,
                error = null
            )

            mediaItem?.localConfiguration?.uri?.let { uri ->
                saveLastTrack(uri.toString(), title)
                viewModelScope.launch(Dispatchers.IO) {
                    val art = extractAlbumArt(uri)
                    _uiState.value = _uiState.value.copy(albumArt = art)
                }
            } ?: run {
                _uiState.value = _uiState.value.copy(albumArt = null)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                updateProgress()
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.value = _uiState.value.copy(isShuffleEnabled = shuffleModeEnabled)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _uiState.value = _uiState.value.copy(repeatMode = repeatMode)
        }
    }

    init {
        loadLastTrackState()

        val token = SessionToken(
            application,
            ComponentName(application, PlaybackService::class.java)
        )
        val future = MediaController.Builder(application, token).buildAsync()
        future.addListener({
            try {
                controller = future.get()
                controller?.addListener(playerListener)
                
                _uiState.value = _uiState.value.copy(
                    isShuffleEnabled = controller?.shuffleModeEnabled ?: false,
                    repeatMode = controller?.repeatMode ?: Player.REPEAT_MODE_OFF
                )
                
                restorePlayerQueue()
                startProgressTracker()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Ошибка подключения к сервису")
            }
        }, MoreExecutors.directExecutor())
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                updateProgress()
                delay(1000L)
            }
        }
    }

    private fun updateProgress() {
        controller?.let {
            _uiState.value = _uiState.value.copy(
                currentPositionMs = it.currentPosition,
                durationMs = if (it.duration == C.TIME_UNSET) 0L else it.duration
            )
        }
    }

    private fun extractAlbumArt(uri: Uri): ByteArray? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(getApplication(), uri)
            val art = retriever.embeddedPicture
            retriever.release()
            art
        } catch (e: Exception) { null }
    }

    private fun loadLastTrackState() {
        viewModelScope.launch {
            val prefs = getApplication<Application>().dataStore.data.first()
            val savedUri = prefs[LAST_TRACK_URI_KEY]
            val savedName = prefs[LAST_TRACK_NAME_KEY] ?: "Нет активного трека"
            if (savedUri != null) {
                _uiState.value = _uiState.value.copy(lastPlayedUri = savedUri, trackName = savedName)
            }
        }
    }

    private fun saveLastTrack(uri: String, name: String) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[LAST_TRACK_URI_KEY] = uri
                prefs[LAST_TRACK_NAME_KEY] = name
            }
        }
    }

    private fun restorePlayerQueue() {
        val savedUri = _uiState.value.lastPlayedUri ?: return
        val savedName = _uiState.value.trackName
        if (controller?.mediaItemCount == 0) {
            controller?.setMediaItem(
                MediaItem.Builder()
                    .setUri(Uri.parse(savedUri))
                    .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(savedName).build())
                    .build()
            )
            controller?.prepare()
        }
    }

    fun loadAudioFiles() {
        try {
            val files = getAudioFiles(getApplication())
            _uiState.value = _uiState.value.copy(audioFiles = files)
        } catch (e: SecurityException) {
            _uiState.value = _uiState.value.copy(error = "Нет доступа к хранилищу")
        }
    }

    fun playTrack(file: AudioFile) {
        try {
            controller?.apply {
                val mediaItems = _uiState.value.audioFiles.map { f ->
                    MediaItem.Builder().setUri(f.uri)
                        .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(f.name.removeSuffix(".mp3")).build())
                        .build()
                }
                val startIndex = _uiState.value.audioFiles.indexOf(file).coerceAtLeast(0)
                
                setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
                prepare()
                play()
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Не удалось воспроизвести: ${file.name}")
        }
    }

    fun togglePlayPause() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun nextTrack() { controller?.seekToNextMediaItem() }
    fun previousTrack() { controller?.seekToPreviousMediaItem() }
    
    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(currentPositionMs = positionMs)
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun toggleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }

    override fun onCleared() {
        progressJob?.cancel()
        controller?.removeListener(playerListener)
        controller?.release()
        super.onCleared()
    }
}
