package com.jlong.miccheck

import android.net.Uri
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material.icons.rounded.MicExternalOn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


@ExperimentalMaterialApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun RecordingsScreen(
    recordings: List<Recording>,
    recordingsData: List<RecordingData>,
    groups: List<RecordingGroup>,
    selectedRecordings: List<Recording>,
    currentPlaybackRec: Recording?,
    onStartPlayback: (Recording) -> Unit,
    onOpenPlayback: () -> Unit,
    onOpenRecordingInfo: (Recording) -> Unit,
    onClickTag: (Tag) -> Unit,
    onSelectRecording: (Recording) -> Unit,
    onCreateGroup: () -> Unit
) {
    var selectedScreen by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(18.dp))
        ScreenSelectRow {
            ScreenSelectButton(
                onClick = { selectedScreen = 0; Log.e("WHICHONE", "RECORDINGS " + selectedScreen) },
                selected = selectedScreen == 0,
                text = "Recordings",
                icon = Icons.Rounded.MicExternalOn
            )
            ScreenSelectButton(
                onClick = { selectedScreen = 1; Log.e("WHICHONE", "GROUPS " + selectedScreen) },
                selected = selectedScreen == 1,
                text = "Groups",
                icon = Icons.Rounded.Inventory2
            )
        }
        Spacer(modifier = Modifier.height(18.dp))

        Crossfade(selectedScreen) {
            if (it == 0)
                RecordingsList(
                    recordings = recordings,
                    recordingsData = recordingsData,
                    selectedRecordings = selectedRecordings,
                    currentPlaybackRec = currentPlaybackRec,
                    onStartPlayback = onStartPlayback,
                    onOpenPlayback = onOpenPlayback,
                    onOpenRecordingInfo = onOpenRecordingInfo,
                    onClickTag = onClickTag,
                    onSelectRecording = onSelectRecording
                )
            else
                GroupsList(
                    recordingsData = recordingsData,
                    groups = groups,
                    onClickGroup = {},
                    onCreateGroup = onCreateGroup
                )
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun RecordingsList(
    recordings: List<Recording>,
    recordingsData: List<RecordingData>,
    selectedRecordings: List<Recording>,
    currentPlaybackRec: Recording?,
    onStartPlayback: (Recording) -> Unit,
    onOpenPlayback: () -> Unit,
    onOpenRecordingInfo: (Recording) -> Unit,
    onClickTag: (Tag) -> Unit,
    onSelectRecording: (Recording) -> Unit
) {

    var recordingsGrouped by remember { mutableStateOf(mapOf<RecordingKey, List<Pair<Recording, RecordingData>>>()) }
    Log.e("RL", "Creating grouped")
    val rec = recordings.toMutableList().apply { sortByDescending { it.uri.toString() } }
    val recData = recordingsData.toMutableList().apply { sortByDescending { it.recordingUri } }
    recordingsGrouped = rec.zip(recData).groupBy { it.first.toDateKey() }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            recordingsGrouped.forEach { (_, recordings) ->
                stickyHeader {
                    DateHeader(recordings[0].first.date)
                }

                itemsIndexed(recordings, key = { _, rec -> rec.first.uri }) { index, item ->
                    Column {
                        RecordingElm(
                            item,
                            onOpenRecordingInfo = onOpenRecordingInfo,
                            onClick = {
                                if (selectedRecordings.isNotEmpty())
                                    onSelectRecording(item.first)
                                else if (item.first == currentPlaybackRec)
                                    onOpenPlayback()
                                else {
                                    onStartPlayback(item.first)
                                    onOpenPlayback()
                                }
                            },
                            onClickTag = onClickTag,
                            isSelectable = selectedRecordings.isNotEmpty(),
                            isSelected = selectedRecordings.contains(item.first),
                            onSelect = onSelectRecording
                        )
                        if (index != recordings.size - 1)
                            Divider(Modifier.padding(18.dp, 0.dp, 0.dp, 0.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(date: LocalDateTime) {
    Surface(
        color = MaterialTheme.colors.background,
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp, 0.dp, 0.dp, 8.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
        ) {
            Text(
                date.format(
                    DateTimeFormatter.ofPattern("LLLL d")
                        .withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault())
                ) +
                        when (date.dayOfMonth % 10) {
                            1 -> if (date.dayOfMonth == 11) "th" else "st"
                            2 -> if (date.dayOfMonth == 12) "th" else "nd"
                            3 -> if (date.dayOfMonth == 13) "th" else "rd"
                            else -> "th"
                        } +
                        date.format(
                            DateTimeFormatter.ofPattern(", yyyy")
                                .withLocale(Locale.getDefault())
                                .withZone(ZoneId.systemDefault())
                        ),
                style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.ExtraBold)
            )
            Spacer(Modifier.height(8.dp))
            Divider()
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Preview
@Composable
fun RecordingScreenPreview() {
    Surface {
        RecordingsScreen(
            recordings = listOf(
                Recording(
                    Uri.EMPTY, "Placeholder 1", 150000, 0, "0B",
                    date = LocalDateTime.now().plusDays(1)
                ),
                Recording(Uri.parse("file:///tmp/android.txt"), "Placeholder 2", 0, 0, "0B"),
                Recording(Uri.parse("file:///tmp/android2.txt"), "Placeholder 3", 0, 0, "0B"),
            ),
            recordingsData = listOf(
                RecordingData(Uri.EMPTY.toString()),
                RecordingData(Uri.parse("file:///tmp/android.txt").toString()),
                RecordingData(Uri.parse("file:///tmp/android2.txt").toString()),
            ),
            groups = listOf(),
            currentPlaybackRec = null,
            onStartPlayback = {},
            onOpenPlayback = { /*TODO*/ },
            onOpenRecordingInfo = {},
            onClickTag = {},
            selectedRecordings = listOf(),
            onSelectRecording = { },
            onCreateGroup = { }
        )
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun RecordingElm(
    rec: Pair<Recording, RecordingData>,
    onOpenRecordingInfo: (Recording) -> Unit,
    onClick: () -> Unit,
    onClickTag: (Tag) -> Unit,
    isSelectable: Boolean,
    isSelected: Boolean,
    onSelect: (Recording) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = 0.dp,
//        onClick = onClick,
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                onSelect(rec.first)
            }
        ),
        backgroundColor = MaterialTheme.colors.background,
        contentColor = MaterialTheme.colors.onBackground
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(18.dp, 18.dp, 0.dp, 18.dp)
        ) {

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Row {
                        Text(
                            rec.first.name,
                            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            " - " + rec.first.duration.toLong().toTimestamp(),
                            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Normal),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        "Recorded " + rec.first.date.format(
                            DateTimeFormatter.ofPattern("LLLL d, h:mm a")
                                .withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault()),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Crossfade(
                    isSelectable,
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 18.dp, 0.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    if (!it)
                        IconButton(
                            onClick = { onOpenRecordingInfo(rec.first) }
                        ) {
                            Icon(Icons.Rounded.Launch, "More Option - Recording")
                        }
                    else
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelect(rec.first) },
                            modifier = Modifier.padding(
                                top = 11.dp,
                                bottom = 14.dp,
                                end = 12.dp,
                                start = 12.dp
                            ),
                            colors = CheckboxDefaults.colors(checkmarkColor = MaterialTheme.colors.background)
                        )
                }
            }

            if (rec.second.tags.isNotEmpty())
                Spacer(Modifier.height(8.dp))
            LazyRow {
                itemsIndexed(rec.second.tags) { index, item ->
                    if (index < 4) {
                        Row {
                            Chip(
                                text = item.name,
                                onClick = { onClickTag(item) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }
            }
        }
    }
}
