package com.example.miccheck

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

class AppViewModel : ViewModel() {
    var currentPlayBackRec by mutableStateOf<Recording?>(null)
        private set
    var currentPlaybackState by mutableStateOf(PlaybackStateCompat.STATE_NONE)
    var currentRecordingUri: Uri? = null
    var recordingState by mutableStateOf(RecordingState.WAITING)
    var selectedScreen by mutableStateOf(0)
        private set
    var selectedBackdrop by mutableStateOf(0)
        private set
    var recordings = mutableStateListOf(Recording(Uri.EMPTY, "PLACEHOLDER", 0, 0))
    var recordingsData = mutableStateListOf<RecordingData>()

    fun setScreen(screen: Int) {
        selectedScreen = screen
    }

    fun setBackdrop(backdrop: Int) {
        selectedBackdrop = backdrop
    }

    fun onRecordingFinished(
        context: Context,
        title: String,
        description: String = ""
    ) {
        //create recording data
        if (currentRecordingUri == null)
            return

        context.contentResolver.update(
            currentRecordingUri!!,
            ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "$title.m4a")
            },
            "${MediaStore.Audio.Media._ID} = ?",
            arrayOf(ContentUris.parseId(currentRecordingUri!!).toString())
        )

        recordingsData.add(
            RecordingData(
                recordingUri = currentRecordingUri.toString(),
                description = description
            )
        )
        currentRecordingUri = null
        recordingState = RecordingState.WAITING
    }

    fun addTagToRecording(recording: Recording) {
        val recData = recordingsData.filter {
            Uri.parse(it.recordingUri) == recording.uri
        }[0]
        val index = recordingsData.indexOf(recData)
        recData.tags += Tag(Random.nextInt().toString())
        recordingsData[index] = recData
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
            MediaStore.Audio.Media.TITLE,
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
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
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
                Log.e("VM", "Found file " + contentUri)
                recordings.add(
                    Recording(
                        contentUri, name, duration, size,
                        date = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                    )
                )

                if (recordingsData.find { contentUri == Uri.parse(it.recordingUri) } == null)
                    recordingsData.add(
                        RecordingData(
                            contentUri.toString()
                        )
                    )
            }
        }
    }
}