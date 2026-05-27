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
