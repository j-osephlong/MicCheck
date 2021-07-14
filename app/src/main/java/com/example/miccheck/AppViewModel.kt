package com.example.miccheck

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

enum class RecordingState {
    RECORDING, PAUSED, WAITING
}

class AppViewModel : ViewModel() {
    var currentPlayBackRec by mutableStateOf<Recording?>(null)
        private set
    var recordingState by mutableStateOf(RecordingState.WAITING)
    var selectedScreen by mutableStateOf(0)
        private set
    var recordings = mutableStateListOf(Recording(Uri.EMPTY, "PLACEHOLDER", 0, 0))
    val recordingsGrouped: Map<RecordingKey, List<Recording>>
        get() = recordings.groupBy { it.toKey() }
    var groups = mutableStateListOf(RecordingGroup("PLACEHOLDER", listOf(), null, Color.White))

    fun setScreen(screen: Int) {
        selectedScreen = screen
    }

    fun addTagToRecording(uri: Uri) {
        val rec = recordings.filter {
            it.uri == uri
        }[0]
        val index = recordings.indexOf(rec)
        rec.data.tags += Tag(Random.nextInt().toString())

        recordings[index] = rec
    }

    fun setCurrentPlayback(rec: Recording?) {
        currentPlayBackRec = rec
    }

    suspend fun loadRecordings(context: Context) {
        Log.i("MC VM", "loadRecordings !!")

        val collection = MediaStore.Audio.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL
        )
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED
        )
        val selection = MediaStore.Audio.Media.RELATIVE_PATH + " like ?"
        val selectionArgs = arrayOf("%micCheck%")
        val sortOrder = MediaStore.Audio.Media.DATE_MODIFIED + " DESC"

        val query = context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        recordings.removeRange(0, recordings.size)

        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                // Get values of columns for a given Audio.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val date = cursor.getInt(dateColumn) * 1000L

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                // Stores column values and the contentUri in a local object
                // that represents the media file.
                recordings.add(
                    Recording(
                        contentUri, name, duration, size,
                        date = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                    )
                )
            }
        }
    }
}