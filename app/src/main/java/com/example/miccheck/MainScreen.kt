package com.example.miccheck

import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
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
    playbackState: Int,
    onStartRecord: () -> Unit,
    onPausePlayRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onFinishedRecording: (String, String) -> Unit,
    onStartPlayback: (Recording) -> Unit,
    onPausePlayPlayback: () -> Unit,
    onAddRecordingTag: (Recording) -> Unit,
    onSelectScreen: (Int) -> Unit,
    onSelectBackdrop: (Int) -> Unit,
    selectedScreen: Int,
    selectedBackdrop: Int,
) {
    var backdropOpen by remember { mutableStateOf(false) }
    val backdropScaffoldState =
        rememberBackdropScaffoldState(
            initialValue = BackdropValue.Concealed,
            confirmStateChange = {
                backdropOpen = it == BackdropValue.Revealed
                it == BackdropValue.Revealed || it == BackdropValue.Concealed
            }
        )

    LaunchedEffect(key1 = backdropOpen)
    {
        if (backdropOpen) {
            backdropScaffoldState.reveal()
        } else {
            backdropScaffoldState.conceal()
        }
    }
    BackdropScaffold(
        scaffoldState = backdropScaffoldState,
        gesturesEnabled = true,
        peekHeight = 72.dp,
        frontLayerElevation = 8.dp,
        frontLayerShape = RoundedCornerShape(22.dp, 0.dp, 0.dp, 0.dp),
        frontLayerBackgroundColor = MaterialTheme.colors.background,
        appBar = {
            TopBar(
                onOpenRecord = { backdropOpen = true; onSelectBackdrop(0) },
                onOpenPlayback = { backdropOpen = true; onSelectBackdrop(1) }
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
                            onOpenPlayback = { onSelectBackdrop(1); backdropOpen = true },
                            onAddRecordingTag = onAddRecordingTag
                        )
                }
            }
        },
        backLayerBackgroundColor = MaterialTheme.colors.primary,
        backLayerContent = {

            Column {
                Crossfade(targetState = selectedBackdrop) {
                    if (it == 0) {
                        RecordingBackdrop(
                            onStartRecord = onStartRecord,
                            onPausePlayRecord = onPausePlayRecord,
                            onStopRecord = onStopRecord,
                            onFinishedRecording = { title, desc ->
                                onFinishedRecording(
                                    title,
                                    desc
                                ); backdropOpen = false
                            },
                            onCancel = { backdropOpen = false },
                            recordingState = recordingState
                        )
                    } else if (it == 1) {
                        PlaybackBackdrop(
                            playbackState = playbackState,
                            currentPlaybackRec = currentPlaybackRec,
                            currentPlaybackRecData =
                            recordingsData.find { recData ->
                                recData.recordingUri == currentPlaybackRec?.uri.toString()
                            },
                            onPausePlayPlayback = onPausePlayPlayback
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun TopBar(
    onOpenRecord: () -> Unit,
    onOpenPlayback: () -> Unit
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
            IconButton(onClick = { onOpenPlayback() }) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Playback",
                )
            }
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
fun MainScreenPreview() {
    var sel by remember {
        mutableStateOf(0)
    }
    val onSelectScreen: (Int) -> Unit = {
        sel = it
    }
    var selB by remember {
        mutableStateOf(0)
    }
    val onSelectBackdrop: (Int) -> Unit = {
        selB = it
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
                playbackState = PlaybackStateCompat.STATE_NONE,
                onStartRecord = { recording = RecordingState.RECORDING },
                onPausePlayRecord = { },
                onStopRecord = { recording = RecordingState.WAITING },
                onFinishedRecording = { _, _ -> },
                onStartPlayback = { /*TODO*/ },
                onPausePlayPlayback = { /*TODO*/ },
                onAddRecordingTag = { /*TODO*/ },
                onSelectScreen = onSelectScreen,
                selectedScreen = sel,
                onSelectBackdrop = onSelectBackdrop,
                selectedBackdrop = selB
            )
        }
    }
}