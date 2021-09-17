package com.jlong.miccheck.ui.compose

import android.net.Uri
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.jlong.miccheck.*
import com.jlong.miccheck.ui.theme.MicCheckTheme
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs

@ExperimentalPagerApi
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun RecordingsScreen(
    pageState: PagerState,
    recordings: List<Recording>,
    recordingsData: List<RecordingData>,
    groups: List<RecordingGroup>,
    selectedRecordings: List<Recording>,
    currentPlaybackRec: Recording?,
    onStartPlayback: (Recording) -> Unit,
    onOpenPlayback: () -> Unit,
    onOpenRecord: () -> Unit,
    onOpenRecordingInfo: (Recording) -> Unit,
    onClickTag: (Tag) -> Unit,
    onSelectRecording: (Recording) -> Unit,
    onClearSelected: () -> Unit,
    onOpenGroup: (RecordingGroup) -> Unit,
    onCreateGroup: () -> Unit,
    showDatePicker: ((Long) -> Unit) -> Unit
) {
    val coroutine = rememberCoroutineScope()
    val setPage: (Int) -> Unit = { page ->
        coroutine.launch {
            pageState.animateScrollToPage(page)
        }
        onClearSelected()
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        if (recordings.isNotEmpty())
            HorizontalPager(
                state = pageState,
                modifier = Modifier.fillMaxSize(),
                dragEnabled = false
            ) { page ->
                when (page) {
                    0 ->
                        Column {
                            ListPageHeader("Recordings", onClick = { setPage(1) }) {
                                Text("Groups >", fontWeight = FontWeight.SemiBold)
                            }
                            RecordingsListSection(
                                recordings = recordings,
                                recordingsData = recordingsData,
                                groups = groups,
                                selectedRecordings = selectedRecordings,
                                currentPlaybackRec = currentPlaybackRec,
                                onStartPlayback = onStartPlayback,
                                onOpenPlayback = onOpenPlayback,
                                onOpenRecordingInfo = onOpenRecordingInfo,
                                onClickTag = onClickTag,
                                onClickGroupTag = onOpenGroup,
                                onSelectRecording = onSelectRecording,
                                showDatePicker = showDatePicker,
                                modifier = Modifier
                                    .fillMaxSize()
                            )
                        }
                    1 ->
                        Column {
                            ListPageHeader("Groups", onClick = { setPage(0) }) {
                                Text("< Recordings", fontWeight = FontWeight.SemiBold)
                            }
                            if (groups.isNotEmpty())
                                GroupsListSection(
                                    recordings = recordings,
                                    recordingsData = recordingsData,
                                    groups = groups,
                                    onClickGroup = onOpenGroup,
                                    modifier = Modifier
                                        .fillMaxSize(),
                                )
                            else
                                NoGroupsScreen {
                                    onCreateGroup()
                                }
                            BackHandler(true) {
                                setPage(0)
                            }
                        }
                }
            }
        else
            NoRecordingsScreen {
                onOpenRecord()
            }
    }
}

@Composable
fun NoRecordingsScreen(onOpenRecord: () -> Unit) {
    Box (Modifier.fillMaxSize()) {
        Column (
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(.75f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "You haven't recorded anything yet 🎙️",
                style = MaterialTheme.typography.h5.copy(color = MaterialTheme.colors.onBackground.copy(alpha = .5f), fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Record something to see it here.",
                style = MaterialTheme.typography.h6.copy(color = MaterialTheme.colors.onBackground.copy(alpha = .5f)),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onOpenRecord) {
                Text(
                    "Record",
                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold)
                )
            }

        }
    }
}

@Composable
fun ListPageHeader(
    text: String,
    onClick: () -> Unit,
    buttonText: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(18.dp, 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.SemiBold))

        TextButton(onClick = onClick, colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.background,
            contentColor = MaterialTheme.colors.primary
        )) {
            buttonText()
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun RecordingsListSection(
    recordings: List<Recording>,
    recordingsData: List<RecordingData>,
    groups: List<RecordingGroup>,
    selectedRecordings: List<Recording>,
    currentPlaybackRec: Recording?,
    onStartPlayback: (Recording) -> Unit,
    onOpenPlayback: () -> Unit,
    onOpenRecordingInfo: (Recording) -> Unit,
    onClickTag: (Tag) -> Unit,
    onClickGroupTag: (RecordingGroup) -> Unit,
    onSelectRecording: (Recording) -> Unit,
    showDatePicker: ((Long) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {

    var recordingsGrouped by remember { mutableStateOf(mapOf<Long, List<Pair<Recording, RecordingData>>>()) }
    Log.e("RL", "Creating grouped")
    val rec = recordings.toMutableList().apply { sortByDescending { it.uri.toString() } }
    val recData = recordingsData.toMutableList().apply { sortByDescending { it.recordingUri } }
    recordingsGrouped = rec.zip(recData)
        .groupBy { it.first.toDateKey() }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val datePickerCallback: (Long) -> Unit = {
        val date = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(it),
            TimeZone.getDefault().toZoneId()
        ).toLocalDate()
        val bestMatch = recordingsGrouped.minByOrNull { match ->
            abs(match.value[0].first.date.toLocalDate().toEpochDay() - date.toEpochDay())
        }
        if (bestMatch != null) {
            val index = recordingsGrouped.toList().indexOf(bestMatch.toPair())
            var sum = 0
            for (i in 0 until index)
                sum += recordingsGrouped.toList()[i].second.size + 1
            coroutineScope.launch {
                listState.scrollToItem(sum)
            }
        }
    }

    Column(modifier) {
        LazyColumn(Modifier.fillMaxSize(), listState) {
            recordingsGrouped
                .toList()
                .sortedByDescending { it.first }
                .forEach { (_, recordings) ->
                    stickyHeader {
                        DateHeader(
                            recordings[0].first.date
                        ) {
                            showDatePicker(datePickerCallback)
                        }
                    }

                    itemsIndexed(recordings, key = { _, rec -> rec.first.uri }) { index, item ->
                        Column {
                        RecordingElm(
                            item,
                            groups.find { it.uuid == item.second.groupUUID},
                            onOpenRecordingInfo = onOpenRecordingInfo,
                            onClick = {
                                when {
                                    selectedRecordings.isNotEmpty() -> onSelectRecording(item.first)
                                    item.first == currentPlaybackRec -> onOpenPlayback()
                                    else -> {
                                        onStartPlayback(item.first)
                                        onOpenPlayback()
                                    }
                                }
                            },
                            onClickTag = onClickTag,
                            isSelectable = selectedRecordings.isNotEmpty(),
                            isSelected = selectedRecordings.contains(item.first),
                            isPlaying = (currentPlaybackRec?.uri == item.first.uri),
                            onSelect = onSelectRecording,
                            onClickGroupTag = onClickGroupTag
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
fun DateHeader(date: LocalDateTime, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colors.background,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(start = 18.dp)
            )
            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(0.dp))
        }
    }
}

@ExperimentalPagerApi
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Preview
@Composable
fun RecordingScreenPreview() {
    MicCheckTheme (false) {
        Surface {
            RecordingElm(
                rec = Pair(Recording(
                        Uri.EMPTY, "Placeholder 1", 150000, 0, "0B",
                        date = LocalDateTime.now().plusDays(1),
                        path = ""
                    ),
                    RecordingData(
                        "",
                        tags = listOf(
                            Tag("Piano"), Tag("Jam"), Tag("SucksSucksSucks"), Tag("Nick Jr")
                        )
                    )
                ),
                group = null,
                onOpenRecordingInfo = {},
                onClick = { /*TODO*/ },
                onClickTag = {},
                onClickGroupTag = {},
                isSelectable = false,
                isSelected = false,
                isPlaying = false,
                onSelect = {}
            )
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun RecordingElm(
    rec: Pair<Recording, RecordingData>,
    group: RecordingGroup?,
    onOpenRecordingInfo: (Recording) -> Unit,
    onClick: () -> Unit,
    onClickTag: (Tag) -> Unit,
    onClickGroupTag: (RecordingGroup) -> Unit,
    isSelectable: Boolean,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelect: (Recording) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colors.primary
) {
    val correctedAccentColor =
        if (accentColor.luminance() > .65 && !isSystemInDarkTheme())
            Color(
                ColorUtils.blendARGB(
                    accentColor.toArgb(),
                    Color.Black.toArgb(),
                    .1f
                )
            )
        else accentColor

    Card(
        elevation = 0.dp,
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
                .padding(18.dp, 24.dp, 0.dp, 24.dp)
        ) {

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    LazyRow {
                        if (group != null)
                            item {
                                Row {
                                    TinyChip(text = group.name, color = Color(group.fallbackColor)) {
                                        onClickGroupTag(group)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                        itemsIndexed(rec.second.tags) { index, item ->
                            if (index < 4) {
                                Row {
                                    TinyChip(
                                        text = item.name,
                                        color = accentColor,
                                        onClick = { onClickTag(item) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                            }
                        }
                    }
                    if (rec.second.tags.isNotEmpty() || group != null)
                        Spacer(Modifier.height(8.dp))
                    Row {
                        Text(
                            rec.first.name,
                            style =
                                MaterialTheme.typography.h6.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPlaying) correctedAccentColor
                                    else MaterialTheme.colors.onBackground
                                ),
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
                            colors = CheckboxDefaults.colors(
                                checkmarkColor =
                                if (!isSystemInDarkTheme() && accentColor.luminance() > .65f)
                                    Color(
                                        ColorUtils.blendARGB(
                                            accentColor.toArgb(),
                                            Color.Black.toArgb(),
                                            .5f
                                        )
                                    )
                                else MaterialTheme.colors.background,
                                checkedColor = accentColor
                            )
                        )
                }
            }



        }
    }
}
