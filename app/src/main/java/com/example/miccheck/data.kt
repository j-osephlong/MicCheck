package com.example.miccheck

import android.net.Uri
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

data class Recording(
    val uri: Uri,
    val name: String,
    val duration: Int,
    // in bytes
    val size: Int,
    val date: LocalDateTime = LocalDateTime.now(),
)

data class RecordingKey(
    val day: Int,
    val year: Int
)

fun Recording.toKey(): RecordingKey {
    return RecordingKey(this.date.dayOfYear, this.date.year)
}

@Serializable
data class RecordingData(
    var recordingUri: String,
    var tags: List<Tag> = listOf(),
    var description: String = ""
)

data class RecordingGroup(
    val name: String,
    val recordings: List<Recording> = listOf<Recording>(),
    var img: Uri? = null,
    var fallbackColor: Color = Color.White
)

@Serializable
data class Tag(
    var name: String,
    val isGroupTag: Boolean = false
)