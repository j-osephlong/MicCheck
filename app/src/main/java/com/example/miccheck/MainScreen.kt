package com.example.miccheck

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    recordings: List<Recording>,
    recordingsData: List<RecordingData>,
    recordingState: RecordingState,
    currentPlaybackRec: Recording?,
    onStartRecord: () -> Unit,
    onPausePlayRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onFinishedRecording: (String, String) -> Unit,
    onStartPlayback: (Recording) -> Unit,
    onStopPlayback: () -> Unit,
    onAddRecordingTag: (Recording) -> Unit,
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
    val onCloseRecord = {
        recordBackdropOpen = false
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
        frontLayerElevation = 8.dp,
        frontLayerShape = RoundedCornerShape(22.dp, 0.dp, 0.dp, 0.dp),
        frontLayerBackgroundColor = MaterialTheme.colors.background,
        appBar = {
            TopBar(
                recordingState = recordingState,
                onOpenRecord = onOpenRecord
            )
        },
        frontLayerContent =
        {
            Column(Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(18.dp))
                ScreenSelectRow {
                    ScreenSelectButton(
                        onClick = { onSelectScreen(0) },
                        selected = selectedScreen == 0,
                        text = "Recordings",
                        icon = Icons.Default.MicExternalOn
                    )
                    ScreenSelectButton(
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
                            recordingsData = recordingsData,
                            currentPlaybackRec = currentPlaybackRec,
                            onStartPlayback = onStartPlayback,
                            onStopPlayback = onStopPlayback,
                            onAddRecordingTag = onAddRecordingTag
                        )
                }
            }
        },
        backLayerBackgroundColor = MaterialTheme.colors.primary,
        backLayerContent = {
            if (currentPlaybackRec != null) {
                Button(onClick = { onStopPlayback() }) {
                    Text("Stop")
                }
            } else if (recordBackdropOpen) {
                RecordingBackdrop(
                    onStartRecord = onStartRecord,
                    onPausePlayRecord = onPausePlayRecord,
                    onStopRecord = onStopRecord,
                    onFinishedRecording = { title, desc ->
                        onFinishedRecording(
                            title,
                            desc
                        ); recordBackdropOpen = false
                    },
                    onCancel = onCloseRecord,
                    recordingState = recordingState
                )
            }
        }
    )
}

@ExperimentalAnimationApi
@Composable
fun RecordingBackdrop(
    onStartRecord: () -> Unit,
    onPausePlayRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onFinishedRecording: (String, String) -> Unit,
    onCancel: () -> Unit,
    recordingState: RecordingState
) {
    val (titleText, setTitleText) = remember { mutableStateOf("New Recording") }
    val (descText, setDescText) = remember { mutableStateOf("") }

    Column(
        Modifier
            .padding(12.dp, 0.dp, 12.dp, 18.dp)
            .animateContentSize()
    ) {
        TextField(
            value = titleText,
            setTitleText,
            modifier = Modifier.fillMaxWidth(),
            colors =
            TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.primaryVariant.copy(
                    alpha = .25f
                ),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color.Black
            ),
            shape = RoundedCornerShape(40),
            textStyle = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(12.dp))
        AnimatedVisibility(visible = recordingState == RecordingState.PAUSED || recordingState == RecordingState.STOPPED) {
            TextField(
                value = descText,
                setDescText,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(.25f),
                label = { Text("Description") },
                colors =
                TextFieldDefaults.textFieldColors(
                    backgroundColor = MaterialTheme.colors.primaryVariant.copy(
                        alpha = .25f
                    ),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.Black
                ),
                singleLine = false,
                shape = RoundedCornerShape(14.dp),
                textStyle = MaterialTheme.typography.body1,
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.animateContentSize()) {
                Row(Modifier.padding(0.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    LargeButton(
                        onClick =
                        {
                            when (recordingState) {
                                RecordingState.WAITING -> {
                                    onStartRecord()
                                }
                                RecordingState.RECORDING, RecordingState.PAUSED -> {
                                    onStopRecord()
                                }
                                RecordingState.STOPPED -> {
                                    onFinishedRecording(titleText, descText)
                                }
                            }
                        }
                    ) {
                        Crossfade(targetState = recordingState) {
                            when (it) {
                                RecordingState.WAITING -> Icon(Icons.Default.Mic, "Record")
                                RecordingState.RECORDING, RecordingState.PAUSED -> Icon(
                                    Icons.Default.Stop,
                                    "Stop"
                                )
                                RecordingState.STOPPED -> Icon(Icons.Default.Check, "Done")
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = recordingState != RecordingState.STOPPED,
                        enter = slideInHorizontally(),
                        exit = slideOutHorizontally()
                    ) {
                        Row {
                            Spacer(Modifier.width(8.dp))
                            CircleButton(
                                onClick =
                                when (recordingState) {
                                    RecordingState.WAITING -> onCancel
                                    else -> onPausePlayRecord
                                }
                            ) {
                                Crossfade(targetState = recordingState) {
                                    when (it) {
                                        RecordingState.WAITING -> Icon(
                                            Icons.Default.Close,
                                            "Cancel"
                                        )
                                        RecordingState.RECORDING -> Icon(
                                            Icons.Default.Pause,
                                            "Pause"
                                        )
                                        else -> Icon(Icons.Default.Mic, "Continue Recording")
                                    }
                                }
                            }
                        }
                    }
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
    recordingState: RecordingState,
    onOpenRecord: () -> Unit,
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = Modifier.height(71.dp),
        backgroundColor = MaterialTheme.colors.primary,
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

@ExperimentalAnimationApi
@Preview
@Composable
fun RecordingControlsPreview() {
    MicCheckTheme {
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colors.primary) {
            RecordingBackdrop(
                onStartRecord = { /*TODO*/ },
                onPausePlayRecord = { /*TODO*/ },
                onStopRecord = { /*TODO*/ },
                onFinishedRecording = { _, _ -> },
                onCancel = { },
                recordingState = RecordingState.STOPPED
            )
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Preview
@Composable
fun MainScreenPreview() {
    var sel by remember {
        mutableStateOf(0)
    }
    val onSelectScreen: (Int) -> Unit = {
        sel = it
    }
    var recording by remember {
        mutableStateOf(RecordingState.WAITING)
    }

    MicCheckTheme {
        Surface {
            MainScreen(
                recordings = listOf(
                    Recording(
                        Uri.EMPTY, "Placeholder 1", 150000, 0,
                        date = LocalDateTime.now().plusDays(1)
                    ),
                    Recording(Uri.parse("file:///tmp/android.txt"), "Placeholder 2", 0, 0),
                    Recording(Uri.parse("file:///tmp/android2.txt"), "Placeholder 3", 0, 0),
                ),
                recordingsData = listOf(
                    RecordingData(Uri.EMPTY.toString()),
                    RecordingData(Uri.parse("file:///tmp/android.txt").toString()),
                    RecordingData(Uri.parse("file:///tmp/android2.txt").toString()),
                ),
                recordingState = recording,
                currentPlaybackRec = null,
                onStartRecord = { recording = RecordingState.RECORDING },
                onPausePlayRecord = { },
                onStopRecord = { recording = RecordingState.WAITING },
                onFinishedRecording = { _, _ -> },
                onStartPlayback = { /*TODO*/ },
                onStopPlayback = { /*TODO*/ },
                onAddRecordingTag = { /*TODO*/ },
                onSelectScreen = onSelectScreen,
                selectedScreen = sel
            )
        }
    }
}