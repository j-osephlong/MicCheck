package com.example.miccheck

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.min

@Composable
fun RecordingsList(
    recordings: List<Recording>,
    onSelectRecording: () -> Unit,
    onAddRecordingTag: (Uri) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(recordings) { index, item ->
                RecordingElm(
                    item,
                    (if (index == 0)
                        Modifier.padding(12.dp, 24.dp, 12.dp, 12.dp)
                    else
                        Modifier.padding(12.dp, 0.dp, 12.dp, 12.dp)),
                    onAddTag = onAddRecordingTag
                )
            }
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

@Composable
fun RecordingElm(
    rec: Recording,
    modifier: Modifier = Modifier,
    onAddTag: (Uri) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val onDismissMenu = {
        menuExpanded = false
    }

    Card(
        elevation = 4.dp,
        modifier = modifier,
        shape = RoundedCornerShape(30)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
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
                            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Text(
                            " - " +
                                    (
                                            (   //hours
                                                    if (((rec.duration / 1000) / 60) / 60 > 0)
                                                        (((rec.duration / 1000) / 360).toString() + ":")
                                                    else
                                                        ""
                                                    ) + //minutes
                                                    ((rec.duration / 1000) / 60) % 60) + ":" +
                                    //seconds
                                    ((rec.duration / 1000) % 60).toString(),
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
