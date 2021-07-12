package com.example.miccheck

import android.content.Context
import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.miccheck.ui.theme.MicCheckTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.*

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun MainScreen(
    recordings: List<Recording>,
    currentlyRecording: Boolean,
    onStartRecord: (ctx: Context) -> Unit,
    onStopRecord: (ctx: Context) -> Unit,
    onSelectRecording: () -> Unit
) {
    Scaffold(
        topBar = { TopBar() },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            if (!currentlyRecording)
                ExtendedFloatingActionButton(
                    text = { Text("Record") },
                    onClick = { /*TODO*/ },
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Record") },
                    shape = RoundedCornerShape(16.dp)
                )
            else
                ExtendedFloatingActionButton(
                    text = { Text("Stop Recording") },
                    onClick = { /*TODO*/ },
                    icon = { Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop") }
                )
        }
    ) {
        Column (Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(8.dp))
            BigButtonRow {
                BigButton(
                    onClick = { /*TODO*/ },
                    selected = true,
                    text = "Recordings",
                    icon = Icons.Default.MicExternalOn
                )
                BigButton(
                    onClick = { /*TODO*/ },
                    selected = false,
                    text = "Groups",
                    icon = Icons.Default.Inventory2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            RecordingsList(
                recordings = recordings,
                onSelectRecording = { /*TODO*/ }
            )
        }
    }
}

@Composable
fun TopBar () {
    var moreMenuExpanded by remember { mutableStateOf(true) }

    TopAppBar(
        modifier = Modifier.height(60.dp),
        backgroundColor = MaterialTheme.colors.background,
        contentColor = MaterialTheme.colors.onBackground,
        elevation = 0.dp,
        title = { Text(
            "micCheck",
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.ExtraBold)) },
        actions = {
            Box(
                Modifier
                    .wrapContentSize(Alignment.TopEnd)
            ) {
                IconButton(onClick = {
                    moreMenuExpanded = true
                }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More Options"
                    )
                }

                DropdownMenu(
                    expanded = moreMenuExpanded,
                    onDismissRequest = { moreMenuExpanded = false },
                ) {
                    DropdownMenuItem(onClick = {
                        moreMenuExpanded = false
                    }) {
                        Text("Temp Item")
                    }
                    DropdownMenuItem(onClick = {
                        moreMenuExpanded = false
                    }) {
                        Text("Temp Item")
                    }
                }
            }
        }
    )
}

@Composable
fun RecordingsList (
    recordings: List<Recording>,
    onSelectRecording: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn (Modifier.fillMaxSize()) {
            items(recordings) {
                item ->
                RecordingElm(item)
            }
        }
    }
}

/**
 * Temporary Debug info
 * Intended params:
 *      - Tags
 *      - Group affil
 *      - Duration
 *      - Description indicator icon
 */
@Composable
fun RecordingElm (
    rec: Recording
) {
    Card (elevation = 8.dp,
    modifier = Modifier.padding(12.dp),
    shape = RoundedCornerShape(30)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                rec.name,
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.ExtraBold)
            )
            Row {
                Text(
                    "Recorded " + rec.date.format(
                        DateTimeFormatter.ofPattern("LLLL d, h:mm a")
                            .withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault())
                    )
                )
                Text(" ∙ ")
                Text(
                    ((
                            if (((rec.duration / 1000) / 60) / 60 > 0)
                                (((rec.duration / 1000) / 360).toString() + ":")
                            else "") +
                            ((rec.duration / 1000) / 60) % 60) + ":" +
                            ((rec.duration / 1000) % 60).toString()
                )
            }
            LazyRow {
                itemsIndexed(rec.data.tags) { _, item ->
                    Text(item.name)
                }
            }
        }
    }
}

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Preview
@Composable
fun MainScreenPreview () {
    MicCheckTheme {
        Surface {
            MainScreen(
                recordings = listOf(
                    Recording(Uri.EMPTY, "Placeholder 1", 150000, 0,
                            data = RecordingData(
                                tags = listOf(
                                    Tag("Tag"),
                                    Tag("Tag1"),
                                    Tag("Tag2"),
                                    Tag("Tag3"),
                                )
                            )
                        ),
                    Recording(Uri.EMPTY, "Placeholder 2", 0, 0),
                    Recording(Uri.EMPTY, "Placeholder 3", 0, 0),
                ),
                currentlyRecording = true,
                onStartRecord = { /*TODO*/ },
                onStopRecord = { /*TODO*/ },
                onSelectRecording = { /*TODO*/ }
            )
        }
    }
}