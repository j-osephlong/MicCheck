package com.jlong.miccheck.ui.compose

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jlong.miccheck.Recording
import com.jlong.miccheck.RecordingData
import com.jlong.miccheck.RecordingGroup
import com.jlong.miccheck.Tag
import com.jlong.miccheck.ui.theme.MicCheckTheme

@Composable
fun PlaybackBackdrop(
    playbackState: Int,
    playbackProgress: Long,
    currentPlaybackRec: Recording?,
    currentPlaybackGroup: RecordingGroup?,
    isGroupPlayback: Boolean,
    onPausePlayPlayback: () -> Unit,
    onSeekPlayback: (Float) -> Unit,
    onSkipPlayback: (Long) -> Unit,
    onOpenRecordingInfo: (Recording) -> Unit,
    onAddRecordingTimestamp: (Recording, Long) -> Unit
) {
    Column(
        Modifier
            .padding(12.dp, 0.dp, 12.dp, 18.dp)
            .animateContentSize()
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                currentPlaybackRec?.name ?: "INSERT CASSETTE",
                style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.ExtraBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable {
                    currentPlaybackRec?.also {
                        onOpenRecordingInfo(it)
                    }
                },
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(20.dp))
        if (currentPlaybackRec != null) {
            Row(horizontalArrangement = Arrangement.Center) {
                Chip(
                    text = currentPlaybackGroup?.name ?: "No Group",
                    onClick = { },
                    color = MaterialTheme.colors.secondary,
                    contentColor = MaterialTheme.colors.onSecondary
                )
                Spacer(Modifier.width(8.dp))
                Chip(
                    text = "Timestamp",
                    onClick = {
                        Log.i("fuck", "$playbackProgress")
                        onAddRecordingTimestamp(currentPlaybackRec, playbackProgress)
                    },
                    color = MaterialTheme.colors.secondary,
                    contentColor = MaterialTheme.colors.onSecondary,
                    icon = Icons.Rounded.Add
                )
            }
//            Spacer(Modifier.height(20.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(playbackProgress.toTimestamp())
            Spacer(Modifier.width(8.dp))
            var valChanging by remember { mutableStateOf(false) }
            var newVal by remember { mutableStateOf(0f) }
            Slider(
                value =
                if (valChanging) newVal
                else if (currentPlaybackRec != null) (
                        playbackProgress / (currentPlaybackRec.duration.toFloat())
                        ) else 0f,
                onValueChange = {
                    valChanging = true
                    newVal = it
                },
                onValueChangeFinished = {
                    onSeekPlayback(newVal)
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            valChanging = false
                        },
                        400
                    )
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colors.primary,
                    activeTrackColor = MaterialTheme.colors.primary,
                    inactiveTrackColor = MaterialTheme.colors.secondary
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text((currentPlaybackRec?.duration?.toLong() ?: 0L).toTimestamp())
        }
//        Spacer(Modifier.height(4.dp))
        PlaybackButtons(
            playbackState = playbackState,
            onPausePlayPlayback = onPausePlayPlayback,
            onSeekDiff = {
                if (currentPlaybackRec != null) {
                    onSeekPlayback(
                        (playbackProgress + it * 1000) / currentPlaybackRec.duration.toFloat()
                    )
                }
            },
            onSkipPlayback = onSkipPlayback,
            recordingLength = currentPlaybackRec?.duration ?: 0,
            isGroupPlayback = isGroupPlayback
        )
    }
}

@Composable
fun PlaybackButtons(
    playbackState: Int,
    onPausePlayPlayback: () -> Unit,
    onSeekDiff: (Int) -> Unit,
    onSkipPlayback: (Long) -> Unit,
    isGroupPlayback: Boolean,
    recordingLength: Int
) {
    Row(modifier = Modifier.animateContentSize()) {
        Row(Modifier.padding(0.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(8.dp))
            if (isGroupPlayback)
            {
                CircleButton(onClick = { onSkipPlayback(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) }) {
                    Icon(
                        Icons.Rounded.SkipPrevious, null
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            CircleButton(onClick = {
                onSeekDiff(
                    if (recordingLength < (1000 * 60 * 1.5))
                        -5
                    else if (recordingLength > (1000 * 60 * 1.5) && recordingLength < (1000 * 60 * 3))
                        -10
                    else
                        -30
                )
            }) {
                Icon(
                    if (recordingLength < (1000 * 60 * 1.5))
                        Icons.Rounded.Replay5
                    else if (recordingLength > (1000 * 60 * 1.5) && recordingLength < (1000 * 60 * 3))
                        Icons.Rounded.Replay10
                    else
                        Icons.Rounded.Replay30,
                    "Replay"
                )
            }
            Spacer(Modifier.width(8.dp))
            LargeButton(onClick = onPausePlayPlayback) {
                Crossfade(targetState = playbackState) {
                    Icon(
                        when (it) {
                            PlaybackStateCompat.STATE_PLAYING -> Icons.Rounded.Pause
                            PlaybackStateCompat.STATE_PAUSED -> Icons.Rounded.PlayArrow
                            PlaybackStateCompat.STATE_STOPPED -> Icons.Rounded.Replay
                            else -> Icons.Rounded.PlayDisabled
                        },
                        "Pause or Play"
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            CircleButton(onClick = {
                onSeekDiff(
                    if (recordingLength < (1000 * 60 * 1.5))
                        5
                    else if (recordingLength > (1000 * 60 * 1.5) && recordingLength < (1000 * 60 * 3))
                        10
                    else
                        30
                )
            }) {
                Icon(
                    if (recordingLength < (1000 * 60 * 1.5))
                        Icons.Rounded.Forward5
                    else if (recordingLength > (1000 * 60 * 1.5) && recordingLength < (1000 * 60 * 3))
                        Icons.Rounded.Forward10
                    else
                        Icons.Rounded.Forward30,
                    "Skip 5"
                )
            }
            if (isGroupPlayback)
            {
                Spacer(Modifier.width(8.dp))
                CircleButton(onClick = { onSkipPlayback(PlaybackStateCompat.ACTION_SKIP_TO_NEXT) }) {
                    Icon(
                        Icons.Rounded.SkipNext, null
                    )
                }
            }
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Preview
@Composable
fun PlaybackControlsPreview() {
    MicCheckTheme {
        Surface(Modifier.fillMaxWidth(), color = Color(0xffFCD9D1)) {
            PlaybackBackdrop(
                playbackState = PlaybackStateCompat.STATE_PAUSED,
                playbackProgress = 0,
                currentPlaybackRec = Recording(
                    uri = Uri.EMPTY,
                    name = "New Recording",
                    duration = 1,
                    size = 0,
                    sizeStr = "0B",
                    path = ""
                ),
                currentPlaybackGroup = null,
                onPausePlayPlayback = { },
                onSeekPlayback = { },
                onOpenRecordingInfo = { },
                onAddRecordingTimestamp = { _, _ -> },
                onSkipPlayback = {},
                isGroupPlayback = true
            )
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Preview
@Composable
fun PlaybackControlsPreviewDark() {
    MicCheckTheme(darkTheme = true) {
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colors.surface) {
            PlaybackBackdrop(
                playbackState = PlaybackStateCompat.STATE_PAUSED,
                playbackProgress = 0,
                currentPlaybackRec = Recording(
                    uri = Uri.EMPTY,
                    name = "New Recording",
                    duration = 1,
                    size = 0,
                    sizeStr = "0B",
                    path = ""
                ),
                currentPlaybackGroup = null,
                onPausePlayPlayback = { },
                onSeekPlayback = { },
                onOpenRecordingInfo = { },
                onAddRecordingTimestamp = { _, _ -> },
                onSkipPlayback = {},
                isGroupPlayback = true
            )
        }
    }
}