package com.example.miccheck

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
    var description: String = "",
    var group: RecordingGroup? = null
)

@Serializable
data class RecordingGroup(
    val name: String,
    var imgUri: String? = null,
    var fallbackColor: Int = Color.White.toArgb()
)

@Serializable
data class Tag(
    var name: String,
)