package com.jlong.miccheck

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.Formatter
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.util.*

class AppViewModel : ViewModel() {
    var settings by mutableStateOf(UserAndSettings())
    var currentPlaybackRec by mutableStateOf<Recording?>(null)
        private set
    var isGroupPlayback by mutableStateOf(false)
    var currentPlaybackState by mutableStateOf(PlaybackStateCompat.STATE_NONE)
    var playbackProgress by mutableStateOf(0L)

    var currentRecordingUri: Uri? = null
    var recordingState by mutableStateOf(RecordingState.WAITING)

    var selectedBackdrop by mutableStateOf(0)
        private set

    var recordings = mutableStateListOf(Recording(Uri.EMPTY, "PLACEHOLDER", 0, 0, "0B", path = ""))
    var recordingsData = mutableStateListOf<RecordingData>()
    var selectedRecordings = mutableStateListOf<Recording>()
    var tags = mutableStateListOf<Tag>()
    var groups = mutableStateListOf<RecordingGroup>()

    lateinit var serializeAndSave: suspend () -> Unit
    lateinit var requestFilePermission: (IntentSender) -> Unit

    var recordTime by mutableStateOf(0L)

    fun onCreateGroup(name: String, imgUri: Uri?, color: Color) {
        var uuid = UUID.randomUUID()
        while (groups.map { it.uuid }.contains(uuid.toString()))
            uuid = UUID.randomUUID()
        val newGroup = RecordingGroup(
            name = name,
            imgUri = imgUri.toString(),
            fallbackColor = color.toArgb(),
            uuid = uuid.toString()
        )

        groups.add(
            newGroup
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun onDeleteGroup(group: RecordingGroup) {
        groups.remove(group)

        val recordings = recordingsData.filter { it.groupUUID == group.uuid}
        recordings.forEach {
            it.groupOrderNumber = -1
            it.groupUUID = null
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun onEditGroup(group: RecordingGroup, name: String? = null, imgUri: String? = null, color: Int? = null) {
        val groupRef = groups.find { it.uuid == group.uuid }!!
        val groupIndex = groups.indexOf(groupRef)

        groupRef.name = name ?: group.name
        groupRef.imgUri = imgUri ?: group.imgUri
        groupRef.fallbackColor = color ?: group.fallbackColor

        groups[groupIndex] = groupRef

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun onRemoveRecordingFromGroup(group: RecordingGroup, recording: Recording) {
        val recData = recordingsData.find {it.recordingUri == recording.uri.toString()}!!
        val recDataIndex = recordingsData.indexOf(recData)

        recData.groupOrderNumber = -1
        recData.groupUUID = null

        recordingsData[recDataIndex] = recData

        orderGroup(group)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun onAddRecordingsToGroup(group: RecordingGroup, recording: Recording?) {
        fun addOne() {
            val recDataRef = recordingsData.find { it.recordingUri == recording!!.uri.toString() }!!
            val recDataIndex = recordingsData.indexOf(recDataRef)

            if (recDataRef.groupUUID == group.uuid)
                return

            val orderNum = recordingsData.filter { it.groupUUID == group.uuid }
                .let {
                    if (it.isEmpty()) 0
                    else it.maxOf { it.groupOrderNumber }+1
                }

            recDataRef.groupUUID = group.uuid
            recDataRef.groupOrderNumber = orderNum
            recordingsData[recDataIndex] = recDataRef

            orderGroup(group)

            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    serializeAndSave()
                }
            }
        }

        if (recording != null) {
            addOne()
            return
        }

        if (selectedRecordings.size < 1)
            return

        selectedRecordings.forEach { rec ->
            val recDataRef = recordingsData.find { it.recordingUri == rec.uri.toString() }!!
            val recDataIndex = recordingsData.indexOf(recDataRef)

            val orderNum = recordingsData.filter { it.groupUUID == group.uuid }
                .let {
                    if (it.isEmpty()) 0
                    else it.maxOf { it.groupOrderNumber }+1
                }
            recDataRef.groupOrderNumber = orderNum
            recDataRef.groupUUID = group.uuid

            recordingsData[recDataIndex] = recDataRef
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun orderGroup (group: RecordingGroup) {
        var i = 0
        val recs = recordingsData.filter { it.groupUUID == group.uuid }
        val ordered = mutableListOf<RecordingData>()
        if (recs.isEmpty())
            return

        var list = recs
        while (list.isNotEmpty())
        {
            val min = list.minByOrNull { it.groupOrderNumber } ?: break
            recordingsData[recordingsData.indexOf(min)].groupOrderNumber = i++
            ordered += min
            list = recs.filter { rec -> ordered.find {it.recordingUri == rec.recordingUri} == null}
        }
    }

    fun onSetBackdrop(backdrop: Int) {
        if (backdrop > 1)
            throw IllegalArgumentException("Invalid backdrop index.")
        selectedBackdrop = backdrop
    }

    fun onSelectRecording(recording: Recording) {
        if (selectedRecordings.contains(recording))
            selectedRecordings.remove(recording)
        else
            selectedRecordings.add(recording)
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
            try {
                context.contentResolver.update(
                    recording.uri,
                    ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, "$title.m4a")
                    },
                    "${MediaStore.Audio.Media._ID} = ?",
                    arrayOf(ContentUris.parseId(recording.uri).toString())
                )
            } catch (securityException: RecoverableSecurityException) {
                val intentSender =
                    securityException.userAction.actionIntent.intentSender
                intentSender.let {
                    requestFilePermission(it)
                }
            }
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

    fun onDeleteRecordings(
        context: Context,
        list: List<Recording>? = null
    ) {
        (list ?: selectedRecordings.also {
            if (it.size < 1)
                return
        }
                ).forEach { recording ->
                val recordingData =
                    recordingsData.find { it.recordingUri == recording.uri.toString() }
                val group = groups.find { it.uuid == recordingData?.groupUUID }
                try {
                    context.contentResolver.delete(
                        recording.uri, null, null
                    )
                } catch (securityException: RecoverableSecurityException) {
                    val intentSender =
                        securityException.userAction.actionIntent.intentSender
                    requestFilePermission(intentSender)
            }

            recordings.remove(recording)
            recordingsData.remove(recordingData)
            if (group != null)
                orderGroup(group)
        }

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
        recordTime = 0L
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
                loadRecordings(context)
            }
        }
    }

    fun onAddTagToRecording(recording: Recording?, tag: Tag) {
        fun tagOne() {
            val recData = recordingsData.find {
                Uri.parse(it.recordingUri) == recording!!.uri
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

        if (recording != null) {
            tagOne()
            return
        }

        if (selectedRecordings.size < 1)
            return

        selectedRecordings.forEach { rec ->
            val recData = recordingsData.find {
                Uri.parse(it.recordingUri) == rec.uri
            }!!
            val index = recordingsData.indexOf(recData)

            if (recData.tags.find { tag.name == it.name } != null)
                return
            if (tags.find { tag.name == it.name } == null)
                tags += tag

            recData.tags += tag
            recordingsData[index] = recData
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun onDeleteTag(recording: Recording, tag: Tag) {
        Log.i("DeleteTag", "Attempting to delete ${tag.name} from ${recording.name}")

        val recData = recordingsData.find {
            Uri.parse(it.recordingUri) == recording.uri
        }!!
        val index = recordingsData.indexOf(recData)

        if (recData.tags.find { tag.name == it.name } == null){
            Log.e("DeleteTag", "No such tag \"${tag.name}\" found on $recording.name}")
            return
        }

        recData.tags -= tag
        recordingsData[index] = recData

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun onAddTimestampToRecording(
        recording: Recording,
        timeMilli: Long,
        title: String,
        description: String?
    ) {
        val recData = recordingsData.find {
            Uri.parse(it.recordingUri) == recording.uri
        }!!
        val index = recordingsData.indexOf(recData)

        if (recData.timeStamps.find { timeMilli == it.timeMilli } != null)
            return

        recData.timeStamps += TimeStamp(
            timeMilli,
            title,
            recording.name,
            recording.uri.toString(),
            description
        )
        recordingsData[index] = recData

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun onDeleteTimestamp(recording: Recording, timeStamp: TimeStamp) {
        val recData = recordingsData.find {
            Uri.parse(it.recordingUri) == recording.uri
        }!!
        val index = recordingsData.indexOf(recData)

        if (recData.timeStamps.find { timeStamp.timeMilli == it.timeMilli } == null){
            Log.e("DeleteTimestamp", "No such stamp \"${timeStamp.name}\" found on ${recording.name}")
            return
        }

        recData.timeStamps -= timeStamp
        recordingsData[index] = recData

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                serializeAndSave()
            }
        }
    }

    fun setCurrentPlayback(rec: Recording?) {
        currentPlaybackRec = rec
        Log.e("VM", rec?.name.toString())
    }

    suspend fun loadRecordings(context: Context) {
        Log.i("MC VM", "loadRecordings !!")

        val collection = MediaStore.Audio.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL
        )
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATA
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
            val displayColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                // Get values of columns for a given Audio.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val date = cursor.getInt(dateColumn) * 1000L
                val dName = cursor.getString(displayColumn)
                val path = cursor.getString(pathColumn)

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                // Stores column values and the contentUri in a local object
                // that represents the media file.
                Log.e("VM", "Found file $contentUri - $name - $dName")
                recordings.add(
                    Recording(
                        contentUri,
                        dName.removeSuffix(".m4a"),
                        duration,
                        size,
                        Formatter.formatShortFileSize(context, size.toLong()).toString(),
                        date = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault())
                            .toLocalDateTime(),
                        path
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