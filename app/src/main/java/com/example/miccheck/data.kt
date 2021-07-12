package com.example.miccheck

import android.accounts.AuthenticatorDescription
import android.net.Uri
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

data class Recording (
    val uri: Uri,
    val name: String,
    val duration: Int,
    // in bytes
    val size: Int,
    val date: LocalDateTime = LocalDateTime.now(),
    val data: RecordingData = RecordingData()
)

data class RecordingData (
    val tags: List<Tag> = listOf(),
    val description: String = "",
)

data class RecordingGroup (
    val name: String,
    val recordings: List<Recording> = listOf<Recording>(),
    val img: Uri? = null,
    val fallbackColor: Color
)

data class Tag (
    val name: String,
    val isGroupTag: Boolean = false
)