package com.example.miccheck

import android.content.ContentUris
import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.miccheck.ui.theme.MicCheckTheme
import java.time.LocalDateTime

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun MainScreen(
    recordings: List<Recording>,
    recordingsData: List<RecordingData>,
    tags: List<Tag>,
    recordingState: RecordingState,
    currentPlaybackRec: Recording?,
    playbackState: Int,
    playbackProgress: Int,
    onStartRecord: () -> Unit,
    onPausePlayRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onFinishedRecording: (String, String) -> Unit,
    onStartPlayback: (Recording) -> Unit,
    onPausePlayPlayback: () -> Unit,
    onSeekPlayback: (Long) -> Unit,
    onAddRecordingTag: (Recording, Tag) -> Unit,
    onEditFinished: (Recording, String, String) -> Unit,
    onDeleteRecording: (Recording) -> Unit,
    onSelectBackdrop: (Int) -> Unit,
    selectedBackdrop: Int,
) {
    val navController = rememberNavController()
    val navState = navController.currentBackStackEntryAsState()
    var backdropOpen by remember { mutableStateOf(false) }
    var backdropTrigger by remember { mutableStateOf(false) }
    val backdropScaffoldState =
        rememberBackdropScaffoldState(
            initialValue = BackdropValue.Concealed,
        )

    val setBackdropOpen: (Boolean) -> Unit = {
        backdropOpen = it
        backdropTrigger = !backdropTrigger
    }

    LaunchedEffect(key1 = backdropOpen, key2 = backdropTrigger)
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
                onOpenBackdrop = { setBackdropOpen(true) }
            )
        },
        frontLayerContent =
        {
            NavHost(navController = navController, startDestination = "recordingsScreen") {
                composable("recordingsScreen") {
                    RecordingsScreen(
                        recordings = recordings,
                        recordingsData = recordingsData,
                        currentPlaybackRec = currentPlaybackRec,
                        onStartPlayback = onStartPlayback,
                        onOpenPlayback = { onSelectBackdrop(1); setBackdropOpen(true) },
                        onOpenRecordingInfo = { recording ->
                            navController.navigate("recordingInfo/" + ContentUris.parseId(recording.uri))
                        }
                    )
                }
                composable("recordingInfo/{uri}") { backStackEntry ->
                    val uri =
                        "content://media/external/audio/media/" + (backStackEntry.arguments?.getString(
                            "uri"
                        ) ?: "")
                    val recording = recordings.find { it.uri == Uri.parse(uri) }
                    val recordingData =
                        recordingsData.find { it.recordingUri == recording?.uri.toString() }
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    RecordingsInfoScreen(
                        recording = recording,
                        recordingData = recordingData,
                        onEditFinished = { title, desc ->
                            recording?.also {
                                onEditFinished(it, title, desc)
                            }
                        },
                        onPlay = {
                            recording?.also {
                                onStartPlayback(it)
                                onSelectBackdrop(1)
                                setBackdropOpen(true)
                            }
                        },
                        onDelete = {
                            showDeleteDialog = true
                        },
                        onAddTag = {
                            recording?.also {
                                navController.navigate("addTag/" + ContentUris.parseId(it.uri))
                            }
                        }
                    )
                    if (showDeleteDialog)
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete recording?") },
                            text = { Text("Deleting is permanent and cannot be undone.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showDeleteDialog = false
                                        navController.navigateUp()
                                        recording?.also {
                                            onDeleteRecording(it)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        contentColor = MaterialTheme.colors.primaryVariant
                                    )
                                ) {
                                    Text("Delete")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showDeleteDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        contentColor = MaterialTheme.colors.onBackground
                                    )
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                }
                composable("addTag/{uri}") { backStackEntry ->
                    val uri =
                        "content://media/external/audio/media/" + (backStackEntry.arguments?.getString(
                            "uri"
                        ) ?: "")
                    val recording = recordings.find { it.uri == Uri.parse(uri) }
                    val recordingData =
                        recordingsData.find { it.recordingUri == recording?.uri.toString() }

                    TagScreen(
                        recording = recording,
                        recordingData = recordingData,
                        tags = tags,
                        onAddTag = { rec, tag ->
                            onAddRecordingTag(rec, tag)
                            navController.navigateUp()
                        }
                    )
                }
            }
        },
        backLayerBackgroundColor = MaterialTheme.colors.primary,
        backLayerContent = {
            Backdrop(
                selectedBackdrop = selectedBackdrop,
                onSelectBackdrop = onSelectBackdrop,
                onStartRecord = onStartRecord,
                onPausePlayRecord = onPausePlayRecord,
                onStopRecord = onStopRecord,
                onFinishedRecording =
                { title, desc ->
                    onFinishedRecording(
                        title,
                        desc
                    )
                    setBackdropOpen(false)
                },
                onCancel = { setBackdropOpen(false) },
                recordingState = recordingState,
                playbackState = playbackState,
                playbackProgress = playbackProgress,
                currentPlaybackRec = currentPlaybackRec,
                currentPlaybackRecData =
                recordingsData.find { recData ->
                    recData.recordingUri == currentPlaybackRec?.uri.toString()
                },
                onPausePlayPlayback = onPausePlayPlayback,
                onSeekPlayback = onSeekPlayback,
                onOpenRecordingInfo = { recording ->
                    navController.navigate("recordingInfo/" + ContentUris.parseId(recording.uri))
                    setBackdropOpen(false)
                }
            )
        }
    )
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
private fun Backdrop(
    selectedBackdrop: Int,
    onSelectBackdrop: (Int) -> Unit,
    onStartRecord: () -> Unit,
    onPausePlayRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onFinishedRecording: (String, String) -> Unit,
    onCancel: () -> Unit,
    recordingState: RecordingState,
    playbackState: Int,
    playbackProgress: Int,
    currentPlaybackRec: Recording?,
    currentPlaybackRecData: RecordingData?,
    onPausePlayPlayback: () -> Unit,
    onSeekPlayback: (Long) -> Unit,
    onOpenRecordingInfo: (Recording) -> Unit
) {
    Column {
        Crossfade(
            targetState = selectedBackdrop,
            modifier = Modifier.animateContentSize()
        ) {
            if (it == 0) {
                RecordingBackdrop(
                    onStartRecord = onStartRecord,
                    onPausePlayRecord = onPausePlayRecord,
                    onStopRecord = onStopRecord,
                    onFinishedRecording = onFinishedRecording,
                    onCancel = onCancel,
                    recordingState = recordingState
                )
            } else if (it == 1) {
                PlaybackBackdrop(
                    playbackState = playbackState,
                    playbackProgress = playbackProgress,
                    currentPlaybackRec = currentPlaybackRec,
                    currentPlaybackRecData = currentPlaybackRecData,
                    onPausePlayPlayback = onPausePlayPlayback,
                    onSeekPlayback = onSeekPlayback,
                    onOpenRecordingInfo = onOpenRecordingInfo
                )
            }
        }
        NewButtons(
            buttonPos = selectedBackdrop,
            onClick = onSelectBackdrop
        )
    }
}

@Composable
fun TopBar(
    onOpenBackdrop: () -> Unit
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = Modifier.height(71.dp),
        backgroundColor = MaterialTheme.colors.primary,
        elevation = 0.dp,
        contentColor = MaterialTheme.colors.onPrimary,
        title = {
            Text(
                "micCheck",
                style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.ExtraBold)
            )
        },
        actions = {
            Box {
                Row(Modifier.align(Alignment.TopEnd)) {
                    IconButton(onOpenBackdrop) {
                        Icon(
                            Icons.Filled.ExpandMore,
                            contentDescription = "Expand Backdrop"
                        )
                    }
                    IconButton(onClick = {
                        moreMenuExpanded = true
                    }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "More Options"
                        )
                    }
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
@ExperimentalAnimationApi
@Preview
@Composable
fun BackdropPreview() {
    MicCheckTheme {
        Surface {
            Backdrop(
                selectedBackdrop = 1,
                onSelectBackdrop = {},
                onStartRecord = { /*TODO*/ },
                onPausePlayRecord = { /*TODO*/ },
                onStopRecord = { /*TODO*/ },
                onFinishedRecording = { _, _ -> },
                onCancel = { /*TODO*/ },
                recordingState = RecordingState.RECORDING,
                playbackState = PlaybackStateCompat.STATE_PLAYING,
                playbackProgress = 0,
                currentPlaybackRec = Recording(
                    Uri.EMPTY,
                    "New Recording New Recording New",
                    0,
                    0,
                    "0B"
                ),
                currentPlaybackRecData = RecordingData(
                    Uri.EMPTY.toString(),
                    listOf(),
                    ""
                ),
                onPausePlayPlayback = { /*TODO*/ },
                onSeekPlayback = { },
                onOpenRecordingInfo = { }
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
                tags = listOf(),
                recordingState = recording,
                currentPlaybackRec = null,
                playbackState = PlaybackStateCompat.STATE_NONE,
                playbackProgress = 0,
                onStartRecord = { recording = RecordingState.RECORDING },
                onPausePlayRecord = { },
                onStopRecord = { recording = RecordingState.WAITING },
                onFinishedRecording = { _, _ -> },
                onStartPlayback = { /*TODO*/ },
                onPausePlayPlayback = { /*TODO*/ },
                onSeekPlayback = { },
                onEditFinished = { _, _, _ -> },
                onAddRecordingTag = { _, _ -> },
                onDeleteRecording = { },
                onSelectBackdrop = onSelectBackdrop,
                selectedBackdrop = selB
            )
        }
    }
}