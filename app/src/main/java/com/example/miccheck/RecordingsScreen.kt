package com.example.miccheck

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.min

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun RecordingsList(
    recordings: Map<RecordingKey, List<Recording>>,
    currentPlaybackRec: Recording?,
    onStartPlayback: (Recording) -> Unit,
    onStopPlayback: () -> Unit,
    onAddRecordingTag: (Uri) -> Unit
) {
//    var currDate: LocalDateTime = LocalDateTime.now().plusDays(1)
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            recordings.forEach { (_, recordings) ->
                stickyHeader {
                    DateHeader(recordings[0].date)
                }

                itemsIndexed(recordings, key = { _, rec -> rec.uri }) { index, item ->
                    if (index == 0)
                        Spacer(modifier = Modifier.height(0.dp))
                    Column {
                        RecordingElm(
                            item,
                            onAddTag = onAddRecordingTag,
                            onClick = {
                                if (item == currentPlaybackRec)
                                    onStopPlayback()
                                else
                                    onStartPlayback(item)
                            }
                        )
                        if (index != recordings.size - 1)
                            Divider(Modifier.padding(18.dp, 0.dp, 0.dp, 0.dp))
                    }
                }
            }

//            itemsIndexed(recordings, key = { _, rec -> rec.uri }) { index, item ->
//                if (index == 0)
//                    DateHeader(item.date)
//                else if (
//                    item.date.dayOfMonth != recordings[index-1].date.dayOfMonth ||
//                    item.date.month != recordings[index-1].date.month           ||
//                    item.date.year != recordings[index-1].date.year)
//                {
//                    DateHeader(item.date)
//                }
//                currDate = item.date
//                RecordingElm(
//                    item,
//                    onAddTag = onAddRecordingTag,
//                    onClick = {
//                        if (item == currentPlaybackRec)
//                            onStopPlayback()
//                        else
//                            onStartPlayback(item)
//                    }
//                )
//            }
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

@Composable
fun RecordingMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAddTag: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            onClick = {
                onAddTag()
                onDismiss()
            }
        ) {
            Text("Add Tag")
        }
    }
}

@ExperimentalMaterialApi
@Composable
fun RecordingElm(
    rec: Recording,
    modifier: Modifier = Modifier,
    onAddTag: (Uri) -> Unit,
    onClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val onDismissMenu = {
        menuExpanded = false
    }

    Card(
        elevation = 0.dp,
        onClick = onClick,
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
                Column {
                    Row {
                        Text(
                            rec.name,
                            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            " - " +
                                    (
                                            if (((rec.duration / 1000) / 60) / 60 > 0)
                                                (((rec.duration / 1000) / 360).toString() + ":")
                                            else
                                                ""
                                            ) + //hours
                                    ((rec.duration / 1000) / 60) % 60 + ":" + //minutes
                                    ((rec.duration / 1000) % 60).toString(), //seconds
                            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Normal)
                        )
                    }

                    Text(
                        "Recorded " + rec.date.format(
                            DateTimeFormatter.ofPattern("LLLL d, h:mm a")
                                .withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault())
                        )
                    )
                }

                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 18.dp, 0.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Icon(Icons.Default.MoreVert, "More Option - Recording")
                    RecordingMenu(
                        expanded = menuExpanded,
                        onDismiss = onDismissMenu,
                        onAddTag = { onAddTag(rec.uri) }
                    )
                }
            }

            if (rec.data.tags.isNotEmpty())
                Spacer(Modifier.height(8.dp))
            LazyRow {
                itemsIndexed(rec.data.tags.subList(0, min(rec.data.tags.size, 4))) { _, item ->
                    Chip(
                        text = item.name,
                        onClick = { }
                    )
                }
            }
        }
    }
}
