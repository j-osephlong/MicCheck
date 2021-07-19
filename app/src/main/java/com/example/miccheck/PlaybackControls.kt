package com.example.miccheck

import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.miccheck.ui.theme.MicCheckTheme

@Composable
fun PlaybackBackdrop(
    playbackState: Int,
    currentPlaybackRec: Recording?,
    currentPlaybackRecData: RecordingData?,
    onPausePlayPlayback: () -> Unit
) {
    Column(
        Modifier
            .padding(12.dp, 0.dp, 12.dp, 18.dp)
            .animateContentSize()
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            currentPlaybackRec?.name ?: "INSERT CASSETTE",
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.ExtraBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(20.dp))
        if (currentPlaybackRec != null) {
            LazyRow(horizontalArrangement = Arrangement.Center) {
                item {
                    Chip(
                        text = currentPlaybackRec.date.year.toString(),
                        onClick = { },
                        color = MaterialTheme.colors.primaryVariant.copy(alpha = .1f),
                        contentColor = MaterialTheme.colors.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Chip(
                        text = currentPlaybackRecData?.group?.name ?: "No Group",
                        onClick = { },
                        color = MaterialTheme.colors.primaryVariant.copy(alpha = .1f),
                        contentColor = MaterialTheme.colors.onPrimary
                    )
                }
                itemsIndexed(currentPlaybackRecData!!.tags) { _, tag ->
                    Row {
                        Spacer(modifier = Modifier.width(8.dp))
                        Chip(tag.name, onClick = { })
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        PlaybackButtons(
            playbackState = playbackState,
            onPausePlayPlayback = onPausePlayPlayback
        )
    }
}

@Composable
fun PlaybackButtons(
    playbackState: Int,
    onPausePlayPlayback: () -> Unit
) {
    Row(modifier = Modifier.animateContentSize()) {
        Row(Modifier.padding(0.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleButton(onClick = { /*TODO*/ }) {
                Icon(
                    Icons.Default.SkipPrevious,
                    "Skip Prev"
                )
            }
            Spacer(Modifier.width(8.dp))
            CircleButton(onClick = { /*TODO*/ }) {
                Icon(
                    Icons.Default.Replay5,
                    "Replay last 5"
                )
            }
            Spacer(Modifier.width(8.dp))
            LargeButton(onClick = onPausePlayPlayback) {
                Crossfade(targetState = playbackState) {
                    Icon(
                        when (it) {
                            PlaybackStateCompat.STATE_PLAYING -> Icons.Default.Pause
                            PlaybackStateCompat.STATE_PAUSED -> Icons.Default.PlayArrow
                            PlaybackStateCompat.STATE_STOPPED -> Icons.Default.Replay
                            else -> Icons.Default.PlayDisabled
                        },
                        "Pause or Play"
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            CircleButton(onClick = { /*TODO*/ }) {
                Icon(
                    Icons.Default.Forward5,
                    "Skip 5"
                )
            }
            Spacer(Modifier.width(8.dp))
            CircleButton(onClick = { /*TODO*/ }) {
                Icon(
                    Icons.Default.SkipNext,
                    "Skip Next"
                )
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
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colors.primary) {
            PlaybackBackdrop(
                playbackState = PlaybackStateCompat.STATE_PAUSED,
                currentPlaybackRec = Recording(
                    uri = Uri.EMPTY,
                    name = "New Recording",
                    duration = 0,
                    size = 0
                ),
                currentPlaybackRecData = RecordingData(
                    recordingUri = Uri.EMPTY.toString(),
                    tags = listOf(
                        Tag("Song1"),
                        Tag("Me"),
                        Tag("2020")
                    )
                ),
                onPausePlayPlayback = { }
            )
        }
    }
}