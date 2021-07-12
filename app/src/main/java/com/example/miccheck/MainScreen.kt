package com.example.miccheck

import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.miccheck.ui.theme.MicCheckTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.min

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun MainScreen(
    recordings: List<Recording>,
    currentlyRecording: Boolean,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onSelectRecording: () -> Unit,
    onSelectScreen: (Int) -> Unit,
    selectedScreen: Int
) {
    Scaffold(
        topBar = { TopBar() },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            if (!currentlyRecording)
                ExtendedFloatingActionButton(
                    text = { Text("Record") },
                    onClick = onStartRecord,
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Record") },
                    shape = RoundedCornerShape(16.dp)
                )
            else
                ExtendedFloatingActionButton(
                    text = { Text("Stop Recording") },
                    onClick = onStopRecord,
                    icon = { Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop") }
                )
        }
    ) {
        Column (Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(8.dp))
            BigButtonRow {
                BigButton(
                    onClick = { onSelectScreen(0) },
                    selected = selectedScreen == 0,
                    text = "Recordings",
                    icon = Icons.Default.MicExternalOn
                )
                BigButton(
                    onClick = { onSelectScreen(1) },
                    selected = selectedScreen == 1,
                    text = "Groups",
                    icon = Icons.Default.Inventory2
                )
            }

            when (selectedScreen) {
                else ->
                    RecordingsList(
                        recordings = recordings,
                        onSelectRecording = { /*TODO*/ }
                    )
            }
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
            itemsIndexed(recordings) { index, item ->
                if (index == 0)
                    RecordingElm(item, Modifier.padding(12.dp, 24.dp, 12.dp, 12.dp))
                else
                    RecordingElm(item, Modifier.padding(12.dp, 0.dp, 12.dp, 12.dp))
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
fun RecordingElm(
    rec: Recording,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = 8.dp,
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
                    onClick = { /*TODO*/ },
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 18.dp, 0.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Icon(Icons.Default.MoreVert, "More Option - Recording")
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

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Preview
@Composable
fun MainScreenPreview () {
    var sel by remember {
        mutableStateOf(0)
    }
    val onSelectScreen: (Int) -> Unit = {
        sel = it
    }

    MicCheckTheme {
        Surface {
            MainScreen(
                recordings = listOf(
                    Recording(
                        Uri.EMPTY, "Placeholder 1", 150000, 0,
                        data = RecordingData(
                            tags = listOf(
                                Tag("Tag"),
                                Tag("Tag1"),
                                Tag("Tag2"),
                                Tag("Tag3Tag3Tag3Tag3"),
                            )
                        )
                    ),
                    Recording(Uri.EMPTY, "Placeholder 2", 0, 0),
                    Recording(Uri.EMPTY, "Placeholder 3", 0, 0),
                ),
                currentlyRecording = true,
                onStartRecord = { /*TODO*/ },
                onStopRecord = { /*TODO*/ },
                onSelectRecording = { /*TODO*/ },
                onSelectScreen = onSelectScreen,
                selectedScreen = sel
            )
        }
    }
}