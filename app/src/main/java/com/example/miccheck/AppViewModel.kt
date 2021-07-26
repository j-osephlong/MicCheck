package com.example.miccheck

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.Formatter
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

class AppViewModel : ViewModel() {
    var currentPlayBackRec by mutableStateOf<Recording?>(null)
        private set
    var currentPlaybackState by mutableStateOf(PlaybackStateCompat.STATE_NONE)
    var currentRecordingUri: Uri? = null
    var recordingState by mutableStateOf(RecordingState.WAITING)
    var selectedBackdrop by mutableStateOf(0)
        private set
    var recordings = mutableStateListOf(Recording(Uri.EMPTY, "PLACEHOLDER", 0, 0, "0B"))
    var recordingsData = mutableStateListOf<RecordingData>()
    var tags = mutableStateListOf<Tag>()

    lateinit var serializeAndSave: suspend () -> Unit

    fun setBackdrop(backdrop: Int) {
        selectedBackdrop = backdrop
    }

    fun onEditRecordingDataFinished(
        context: Context,
        recording: Recording,
        title: String,
        description: String
    ) {
        val recordingIndex = recordings.indexOf(recording)
        val recordingRef = recordings[recordingIndex]
        val recordingData = recordingsData.find { it.recordingUri == recording.uri.toString() }!!
        val recordingDataIndex = recordingsData.indexOf(recordingData)

        if (title.isNotBlank()) {
            context.contentResolver.update(
                recording.uri,
                ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, "$title.m4a")
                },
                "${MediaStore.Audio.Media._ID} = ?",
                arrayOf(ContentUris.parseId(recording.uri).toString())
            )
            recordingRef.name = title
        }

        recordingData.description = description

        recordings[recordingIndex] = recordingRef
        recordingsData[recordingDataIndex] = recordingData

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun onDeleteRecording(
        context: Context,
        activity: Activity,
        recording: Recording
    ) {
        val recordingData = recordingsData.find { it.recordingUri == recording.uri.toString() }
        try {
            context.contentResolver.delete(
                recording.uri, null, null
            )
        } catch (securityException: RecoverableSecurityException) {
            val intentSender =
                securityException.userAction.actionIntent.intentSender
            intentSender?.let {
                startIntentSenderForResult(
                    activity,
                    it,
                    1020,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            }
        }

        recordings.remove(recording)
        recordingsData.remove(recordingData)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
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

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun addTagToRecording(recording: Recording, tag: Tag) {
        val recData = recordingsData.find {
            Uri.parse(it.recordingUri) == recording.uri
        }!!
        val index = recordingsData.indexOf(recData)

        if (recData.tags.find { tag.name == it.name } != null)
            return
        if (tags.find { tag.name == it.name } == null)
            tags += tag

        recData.tags += tag
        recordingsData[index] = recData

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
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
                        contentUri,
                        name,
                        duration,
                        size,
                        Formatter.formatShortFileSize(context, size.toLong()).toString(),
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

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }
}