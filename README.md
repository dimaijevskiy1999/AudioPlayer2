# 🎵 AudioPlayer — Полный код + Инструкция для GitHub

---

## 📁 СТРУКТУРА ПРОЕКТА

```
audioplayer/
├── .github/
│   └── workflows/
│       └── build.yml                          ← Автосборка APK
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           └── java/com/example/audioplayer/
│               ├── MainActivity.kt
│               ├── PlaybackService.kt
│               ├── PlayerScreen.kt
│               ├── PlayerViewModel.kt
│               ├── AudioRepository.kt
│               └── ui/
│                   ├── theme/
│                   │   ├── Color.kt
│                   │   └── Theme.kt
│                   └── components/
│                       ├── PlayerControls.kt
│                       └── TrackList.kt
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/
    └── wrapper/
        └── gradle-wrapper.properties
```

---

## 📄 ФАЙЛЫ ПРОЕКТА

---

### `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AudioPlayer"
include(":app")
```

---

### `build.gradle.kts` (корневой)

```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
```

---

### `gradle/wrapper/gradle-wrapper.properties`

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

---

### `app/build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.audioplayer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.audioplayer"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Lifecycle + ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Media3 (ExoPlayer + Session)
    implementation("androidx.media3:media3-exoplayer:1.3.0")
    implementation("androidx.media3:media3-session:1.3.0")
    implementation("androidx.media3:media3-common:1.3.0")
}
```

---

### `app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

    <application
        android:allowBackup="true"
        android:label="AudioPlayer"
        android:theme="@style/Theme.AppCompat.DayNight.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".PlaybackService"
            android:foregroundServiceType="mediaPlayback"
            android:exported="true">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>

    </application>

</manifest>
```

---

### `ui/theme/Color.kt`

```kotlin
package com.example.audioplayer.ui.theme

import androidx.compose.ui.graphics.Color

val Black      = Color(0xFF000000)
val DarkGray   = Color(0xFF1A1A1A)
val MediumGray = Color(0xFF2C2C2C)
val Red        = Color(0xFFE53935)
val DarkRed    = Color(0xFFB71C1C)
val White      = Color(0xFFFFFFFF)
val LightGray  = Color(0xFFBDBDBD)
```

---

### `ui/theme/Theme.kt`

```kotlin
package com.example.audioplayer.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary          = Red,
    onPrimary        = White,
    secondary        = DarkRed,
    onSecondary      = White,
    background       = Black,
    onBackground     = White,
    surface          = DarkGray,
    onSurface        = White,
    surfaceVariant   = MediumGray,
    onSurfaceVariant = LightGray
)

@Composable
fun AudioPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
```

---

### `AudioRepository.kt`

```kotlin
package com.example.audioplayer

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class AudioFile(
    val id: Long,
    val name: String,
    val uri: Uri
)

fun getAudioFiles(context: Context): List<AudioFile> {
    val audioList = mutableListOf<AudioFile>()
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME
    )

    context.contentResolver.query(
        collection, projection, null, null,
        "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"
    )?.use { cursor ->
        val idCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

        while (cursor.moveToNext()) {
            val id   = cursor.getLong(idCol)
            val name = cursor.getString(nameCol)
            val uri  = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
            )
            audioList.add(AudioFile(id, name, uri))
        }
    }
    return audioList
}
```

---

### `PlaybackService.kt`

```kotlin
package com.example.audioplayer

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        player       = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player!!).build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
```

---

### `PlayerViewModel.kt`

```kotlin
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
```

---

### `ui/components/TrackList.kt`

```kotlin
package com.example.audioplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audioplayer.AudioFile

@Composable
fun TrackList(
    files: List<AudioFile>,
    currentTrackName: String,
    onTrackClick: (AudioFile) -> Unit
) {
    if (files.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет доступных аудиофайлов.\nПредоставьте доступ к хранилищу.",
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        items(files) { file ->
            val cleanName  = file.name.removeSuffix(".mp3")
            val isSelected = cleanName == currentTrackName

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { onTrackClick(file) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF8B0000) else Color(0xFF1A1A1A)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "♪",
                        color = if (isSelected) Color.White else Color(0xFFE53935),
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = cleanName,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
```

---

### `ui/components/PlayerControls.kt`

```kotlin
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
```

---

### `PlayerScreen.kt`

```kotlin
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
    }

    // Запрашиваем разрешение при старте
    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(permission)
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // Заголовок
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

        // Контент
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

        // Панель управления
        PlayerControls(
            state = uiState,
            onPlayPause = { viewModel.togglePlayPause() },
            onNext = { viewModel.nextTrack() }
        )

        // Уведомление об ошибке
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
                Text(uiState.error, color = Color.White)
            }
        }
    }
}
```

---

### `MainActivity.kt`

```kotlin
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
```

---

### `.github/workflows/build.yml`

```yaml
name: Build Debug APK

on:
  push:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build Debug APK
        run: ./gradlew assembleDebug

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: AudioPlayer-Debug
          path: app/build/outputs/apk/debug/app-debug.apk
```

---

---

# 🛠 ИНСТРУКЦИЯ: КАК СОЗДАТЬ ПРОЕКТ ЧЕРЕЗ GITHUB.COM (ТОЛЬКО БРАУЗЕР)

## ШАГ 1 — Создать репозиторий

1. Перейти на **github.com** → войти в аккаунт.
2. Нажать кнопку **"New"** (зелёная, вверху слева).
3. Заполнить:
   - **Repository name:** `AudioPlayer`
   - Выбрать **Private** или **Public** — на ваш выбор.
   - ✅ Поставить галочку **"Add a README file"**.
4. Нажать **"Create repository"**.

---

## ШАГ 2 — Создавать файлы через веб-редактор

Для каждого файла из списка выше:

1. На странице репозитория нажать кнопку **"Add file"** → **"Create new file"**.
2. В поле **Name your file...** ввести путь файла, например:
   ```
   app/src/main/java/com/example/audioplayer/MainActivity.kt
   ```
   *(GitHub автоматически создаст все папки при вводе `/`)*
3. В редакторе вставить код из соответствующего раздела выше.
4. Внизу страницы нажать **"Commit new file"**.

**Повторить для каждого файла из структуры проекта.**

### Порядок создания файлов (рекомендуемый):

| № | Путь файла |
|---|---|
| 1 | `settings.gradle.kts` |
| 2 | `build.gradle.kts` |
| 3 | `gradle/wrapper/gradle-wrapper.properties` |
| 4 | `app/build.gradle.kts` |
| 5 | `app/src/main/AndroidManifest.xml` |
| 6 | `app/src/main/java/com/example/audioplayer/ui/theme/Color.kt` |
| 7 | `app/src/main/java/com/example/audioplayer/ui/theme/Theme.kt` |
| 8 | `app/src/main/java/com/example/audioplayer/AudioRepository.kt` |
| 9 | `app/src/main/java/com/example/audioplayer/PlaybackService.kt` |
| 10 | `app/src/main/java/com/example/audioplayer/PlayerViewModel.kt` |
| 11 | `app/src/main/java/com/example/audioplayer/ui/components/TrackList.kt` |
| 12 | `app/src/main/java/com/example/audioplayer/ui/components/PlayerControls.kt` |
| 13 | `app/src/main/java/com/example/audioplayer/PlayerScreen.kt` |
| 14 | `app/src/main/java/com/example/audioplayer/MainActivity.kt` |
| 15 | `.github/workflows/build.yml` |

---

## ШАГ 3 — Добавить Gradle Wrapper (обязательно!)

Без исполняемого файла `gradlew` сборка не запустится.

1. Создать файл `gradle/wrapper/gradle-wrapper.properties` с содержимым из раздела выше.
2. Создать **пустой** файл `gradlew` (одна пустая строка) — мы заменим его ниже.
3. Открыть **Settings** репозитория → **Actions** → убедиться, что Actions включены.

> **Важно:** Файл `gradlew` — исполняемый bash-скрипт. Стандартный скрипт можно взять с официального репозитория Google:  
> Создайте файл `gradlew` и вставьте содержимое отсюда:  
> `https://raw.githubusercontent.com/gradle/gradle/master/gradlew`

---

## ШАГ 4 — Запустить сборку (GitHub Actions)

После того как файл `.github/workflows/build.yml` будет создан и закоммичен:

1. Перейти во вкладку **"Actions"** в репозитории.
2. Вы увидите workflow **"Build Debug APK"**.
3. Нажать на него → **"Run workflow"** → **"Run workflow"** (зелёная кнопка).
4. Дождаться завершения сборки (обычно 3–7 минут).

---

## ШАГ 5 — Скачать APK

1. После завершения сборки нажать на зелёную галочку ✅ или зайти в **Actions**.
2. Открыть завершённый workflow run.
3. В разделе **"Artifacts"** внизу страницы нажать **"AudioPlayer-Debug"**.
4. Скачается ZIP-архив с файлом `app-debug.apk`.
5. Распаковать архив, перенести APK на телефон и установить.

> ⚠️ Для установки APK на телефоне нужно включить **"Установка из неизвестных источников"** в настройках Android.

---

## 📌 Итоговая схема

```
Редактируете файлы на github.com
         ↓
  Commit → push в ветку main
         ↓
  GitHub Actions запускает сборку
  (./gradlew assembleDebug на сервере Ubuntu)
         ↓
  APK готов → скачиваете → устанавливаете
```
