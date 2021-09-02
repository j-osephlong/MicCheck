package com.jlong.miccheck

import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

interface Searchable {
    val name: String
}


fun Recording.toDateKey(): Long =
    this.date.toLocalDate().toEpochDay()

data class Recording(
    val uri: Uri,
    override var name: String,
    val duration: Int,
    // in bytes
    val size: Int,
    val sizeStr: String,
    val date: LocalDateTime = LocalDateTime.now(),
    val path: String
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

    /**
     * V2 - Group order value
     */
    @Serializable
    data class V2(
        var recordingUri: String,
        var tags: List<Tag> = listOf(),
        var description: String = "",
        var groupUUID: String? = null,
        var groupOrderNumber: Int = -1,
        var timeStamps: List<TimeStamp> = listOf()
    ) : VersionedRecordingData()

    /**
     * V3 - Clip parent URI
     */
    @Serializable
    data class V3(
        var recordingUri: String,
        var tags: List<Tag> = listOf(),
        var description: String = "",
        var groupUUID: String? = null,
        var groupOrderNumber: Int = -1,
        var timeStamps: List<TimeStamp> = listOf(),
        var clipParentUri: String? = null
    ) : VersionedRecordingData()
}

fun VersionedRecordingData.toLatestVersion(): RecordingData = when (this) {
    is VersionedRecordingData.V1 -> this.let {
        RecordingData(
            recordingUri = it.recordingUri,
            tags = it.tags.let { oldTags ->
                val newTags = mutableListOf<Tag>()
                oldTags.forEach { tag -> newTags += tag.toLatestVersion() }
                newTags
            },
            description = it.description,
            groupUUID = it.group?.uuid,
            groupOrderNumber = 0,
            timeStamps = it.timeStamps.let { oldStamps ->
                val newStamps = mutableListOf<TimeStamp>()
                oldStamps.forEach { stamp -> newStamps += stamp.toLatestVersion() }
                newStamps
            },
            clipParentUri = null
        )
    }
    is VersionedRecordingData.V2 -> this.let {
        RecordingData(
            recordingUri = it.recordingUri,
            tags = it.tags.let { oldTags ->
                val newTags = mutableListOf<Tag>()
                oldTags.forEach { tag -> newTags += tag.toLatestVersion() }
                newTags
            },
            description = it.description,
            groupUUID = it.groupUUID,
            groupOrderNumber = 0,
            timeStamps = it.timeStamps.let { oldStamps ->
                val newStamps = mutableListOf<TimeStamp>()
                oldStamps.forEach { stamp -> newStamps += stamp.toLatestVersion() }
                newStamps
            },
            clipParentUri = null
        )
    }
    is VersionedRecordingData.V3 -> this
}

typealias RecordingData = VersionedRecordingData.V3

@Serializable
sealed class VersionedRecordingGroup {
    @Serializable
    data class V2(
        val name: String,
        var imgUri: String? = null,
        var fallbackColor: Int = Color.White.toArgb(),
        val uuid: String,
    ) : VersionedRecordingGroup()

    /**
     * V3 - Name is now mutable
     *    - Searchable
     */
    @Serializable
    data class V3(
        override var name: String,
        var imgUri: String? = null,
        var fallbackColor: Int = Color.White.toArgb(),
        val uuid: String,
    ) : VersionedRecordingGroup(), Searchable
}

fun VersionedRecordingGroup.toLatestVersion(): RecordingGroup = when (this) {
    is VersionedRecordingGroup.V2 ->
        RecordingGroup(
            this.name,
            this.imgUri,
            this.fallbackColor,
            this.uuid
        )
    is VersionedRecordingGroup.V3 -> this
}

typealias RecordingGroup = VersionedRecordingGroup.V3

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

@Serializable
data class UserAndSettings (
    val firstLaunch: Boolean = true,
    val firstGroups: Boolean = true,
    val sampleRate: Int = 48000,
    val encodingBitRate: Int = 384000
)

fun Pair<Recording, RecordingGroup?>.toMetaData(): Bundle =
    Bundle().apply {
        putString(MediaMetadataCompat.METADATA_KEY_TITLE, this@toMetaData.first.name)
        putString(
            MediaMetadataCompat.METADATA_KEY_ALBUM,
            this@toMetaData.second?.name ?: "No Group"
        )
        putString(
            MediaMetadataCompat.METADATA_KEY_ARTIST,
            "Me" /*TODO*/
        )
        putLong(
            MediaMetadataCompat.METADATA_KEY_DURATION,
            this@toMetaData.first.duration.toLong()
        )
        if (this@toMetaData.second?.imgUri != null)
            putString(
                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                this@toMetaData.second!!.imgUri.toString()
            )
        if (this@toMetaData.second != null)
            putInt(
                "CUSTOM_KEY_COLOR",
                this@toMetaData.second!!.fallbackColor
            )
    }