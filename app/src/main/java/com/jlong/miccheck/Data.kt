package com.jlong.miccheck

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

const val recordingDataVersion = 1
const val recordingGroupVersion = 2
const val tagVersion = 1
const val timeStampVersion = 1

interface Searchable {
    val name: String
}

data class RecordingKey(
    val day: Int,
    val year: Int
)

fun Recording.toDateKey(): RecordingKey {
    return RecordingKey(this.date.dayOfYear, this.date.year)
}

data class Recording(
    val uri: Uri,
    override var name: String,
    val duration: Int,
    // in bytes
    val size: Int,
    val sizeStr: String,
    val date: LocalDateTime = LocalDateTime.now(),
) : Searchable

@Serializable
sealed class VersionedRecordingData {
    @Serializable
    data class V1(
        var recordingUri: String,
        var tags: List<Tag> = listOf(),
        var description: String = "",
        var group: RecordingGroup? = null,
        var timeStamps: List<TimeStamp> = listOf()
    ) : VersionedRecordingData()
}

fun VersionedRecordingData.toLatestVersion(): RecordingData = when (this) {
    is VersionedRecordingData.V1 -> this
}

typealias RecordingData = VersionedRecordingData.V1

@Serializable
sealed class VersionedRecordingGroup {
    @Serializable
    data class V1(
        val name: String,
        var imgUri: String? = null,
        var fallbackColor: Int = Color.White.toArgb(),
        val uuid: String,
    ) : VersionedRecordingGroup()

    @Serializable
    data class V2(
        val name: String,
        var imgUri: String? = null,
        var fallbackColor: Int = Color.White.toArgb(),
        val uuid: String,
    ) : VersionedRecordingGroup()
}

fun VersionedRecordingGroup.toLatestVersion(): RecordingGroup = when (this) {
    is VersionedRecordingGroup.V2 -> this
    is VersionedRecordingGroup.V1 -> this.let {
        RecordingGroup(
            it.name,
            it.imgUri,
            it.fallbackColor,
            it.uuid
        )
    }
}

typealias RecordingGroup = VersionedRecordingGroup.V2

@Serializable
sealed class VersionedTag {
    @Serializable
    data class V1(
        var name: String,
    ) : VersionedTag()
}

fun VersionedTag.toLatestVersion(): Tag = when (this) {
    is VersionedTag.V1 -> this
}

typealias Tag = VersionedTag.V1

@Serializable
sealed class VersionedTimeStamp {
    @Serializable
    data class V1(
        val timeMilli: Long,
        override var name: String,
        var recordingName: String,
        var recordingUri: String,
        var description: String? = null
    ) : Searchable, VersionedTimeStamp()
}

fun VersionedTimeStamp.toLatestVersion(): TimeStamp = when (this) {
    is VersionedTimeStamp.V1 -> this
}

typealias TimeStamp = VersionedTimeStamp.V1

@Serializable
data class PackagedData(
    val groups: List<VersionedRecordingGroup>,
    val tags: List<VersionedTag>,
    val recordingsData: List<VersionedRecordingData>
)