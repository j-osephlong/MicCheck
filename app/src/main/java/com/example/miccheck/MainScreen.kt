package com.example.miccheck

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.miccheck.ui.theme.MicCheckTheme
import java.time.LocalDateTime

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun MainScreen(
    recordings: Map<RecordingKey, List<Recording>>,
    currentlyRecording: Boolean,
    currentPlaybackRec: Recording?,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onStartPlayback: (Recording) -> Unit,
    onStopPlayback: () -> Unit,
    onAddRecordingTag: (Uri) -> Unit,
    onSelectScreen: (Int) -> Unit,
    selectedScreen: Int,
) {
    var recordBackdropOpen by remember { mutableStateOf(false) }
    val backdropScaffoldState =
        rememberBackdropScaffoldState(
            initialValue = BackdropValue.Concealed
        )
    val onOpenRecord = {
        recordBackdropOpen = true
    }

    LaunchedEffect(key1 = recordBackdropOpen || currentPlaybackRec != null)
    {
        if (recordBackdropOpen || currentPlaybackRec != null) {
            backdropScaffoldState.reveal()
        } else {
            backdropScaffoldState.conceal()
        }
    }
    BackdropScaffold(
        scaffoldState = backdropScaffoldState,
        gesturesEnabled = recordBackdropOpen || currentPlaybackRec != null,
        peekHeight = 72.dp,
        frontLayerElevation = 12.dp,
        frontLayerShape = RoundedCornerShape(22.dp, 0.dp, 0.dp, 0.dp),
        frontLayerBackgroundColor = MaterialTheme.colors.background,
        appBar = {
            TopBar(
                currentlyRecording = currentlyRecording,
                onOpenRecord = onOpenRecord
            )
        },
        frontLayerContent =
        {
            Column(Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(18.dp))
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
                Spacer(modifier = Modifier.height(12.dp))

                when (selectedScreen) {
                    else ->
                        RecordingsList(
                            recordings = recordings,
                            currentPlaybackRec = currentPlaybackRec,
                            onStartPlayback = onStartPlayback,
                            onStopPlayback = onStopPlayback,
                            onAddRecordingTag = onAddRecordingTag
                        )
                }
            }
        },
        backLayerBackgroundColor = Color(0xfffbe9e7),
        backLayerContent = {
            if (currentPlaybackRec != null) {
                Button(onClick = { onStopPlayback() }) {
                    Text("Stop")
                }
            } else if (recordBackdropOpen) {
                RecordingBackdrop(
                    onStartRecord = onStartRecord,
                    onPausePlay = { /*TODO*/ },
                    onStopRecord = { onStopRecord(); recordBackdropOpen = false },
                    currentlyRecording = currentlyRecording
                )
            }
        }
    )
}

@Composable
fun RecordingBackdrop(
    onStartRecord: () -> Unit,
    onPausePlay: () -> Unit,
    onStopRecord: () -> Unit,
    currentlyRecording: Boolean
) {
    val (titleText, setTitleText) = remember { mutableStateOf("") }
    val (descText, setDescText) = remember { mutableStateOf("") }

    Column(Modifier.padding(12.dp)) {
        TextField(
            value = titleText,
            setTitleText,
            textStyle = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(12.dp))
        TextField(
            value = descText,
            setDescText,
            singleLine = false,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(.3f)
        )
        Spacer(Modifier.height(12.dp))
        Crossfade(targetState = currentlyRecording) {
            if (it) {
                Row {
                    IconButton(onClick = onPausePlay) {
                        Icon(Icons.Default.Pause, "Pause")
                    }
                    IconButton(onClick = onStopRecord) {
                        Icon(Icons.Default.Stop, "Stop")
                    }
                }
            } else {
                IconButton(onClick = onStartRecord) {
                    Icon(Icons.Default.RadioButtonChecked, "Record")
                }
            }
        }
    }
}

/*
floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text =
                { Text(if (!currentlyRecording) "Record" else "Stop Recording", modifier = Modifier.animateContentSize()) },
                onClick = if (!currentlyRecording) onStartRecord else onStopRecord,
                icon =
                {
                    Crossfade(targetState = currentlyRecording) {
                        Icon(
                            imageVector = if (!it) Icons.Default.Add else Icons.Default.Stop,
                            contentDescription = if (!it) "Record" else "Stop Recording"
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        },
*/


@Composable
fun TopBar(
    currentlyRecording: Boolean,
    onOpenRecord: () -> Unit,
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = Modifier.height(71.dp),
        backgroundColor = Color(0xfffbe9e7),
        elevation = 0.dp,
        contentColor = MaterialTheme.colors.onBackground,
        title = {
            Text(
                "micCheck",
                style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.ExtraBold)
            )
        },
        actions = {
            IconButton(onClick = { onOpenRecord() }) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Record",
                )
            }
            IconButton(onClick = { }) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "Search"
                )
            }
            Box {
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

@ExperimentalFoundationApi
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
    var recording by remember {
        mutableStateOf(false)
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
                        ),
                        date = LocalDateTime.now().plusDays(1)
                    ),
                    Recording(Uri.parse("file:///tmp/android.txt"), "Placeholder 2", 0, 0),
                    Recording(Uri.parse("file:///tmp/android2.txt"), "Placeholder 3", 0, 0),
                ).groupBy { it.toKey() },
                currentlyRecording = recording,
                currentPlaybackRec = null,
                onStartRecord = { recording = true },
                onStopRecord = { recording = false },
                onStartPlayback = { /*TODO*/ },
                onStopPlayback = { /*TODO*/ },
                onAddRecordingTag = { /*TODO*/ },
                onSelectScreen = onSelectScreen,
                selectedScreen = sel
            )
        }
    }
}