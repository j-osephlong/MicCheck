package com.jlong.miccheck

import android.content.ContentUris
import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jlong.miccheck.ui.theme.MicCheckTheme

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun AppUI(
    recordings: List<Recording>,
    recordingsData: List<RecordingData>,
    tags: List<Tag>,
    groups: List<RecordingGroup>,
    recordingState: RecordingState,
    currentPlaybackRec: Recording?,
    playbackState: Int,
    playbackProgress: Long,
    elapsedRecordingTime: Long,
    onStartRecord: () -> Unit,
    onPausePlayRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onFinishedRecording: (String, String) -> Unit,
    onCreateGroup: (String, Uri?) -> Unit,
    onAddRecordingsToGroup: (RecordingGroup, Recording?) -> Unit,
    onStartPlayback: (Recording) -> Unit,
    onPausePlayPlayback: () -> Unit,
    onSeekPlayback: (Float) -> Unit,
    onAddRecordingTag: (Recording?, Tag) -> Unit,
    onDeleteTag: (Recording, Tag) -> Unit,
    onAddRecordingTimestamp: (Recording, Long, String, String?) -> Unit,
    onDeleteTimestamp: (Recording, TimeStamp) -> Unit,
    onEditFinished: (Recording, String, String) -> Unit,
    onShareRecordings: (Recording?) -> Unit,
    onDeleteRecordings: (List<Recording>?) -> Unit,
    onSelectBackdrop: (Int) -> Unit,
    selectedBackdrop: Int,
    selectedRecordings: List<Recording>,
    onSelectRecording: (Recording) -> Unit,
    onClearSelected: () -> Unit,
    onChooseImage: ((Uri) -> Unit) -> Unit
) {
    val navController = rememberNavController()
    val navState = navController.currentBackStackEntryAsState()
    var backdropOpen by remember { mutableStateOf(false) }
    var backdropTrigger by remember { mutableStateOf(false) }
    val backdropScaffoldState =
        rememberBackdropScaffoldState(
            initialValue = BackdropValue.Concealed,
        )
    var selectedSearchTagFilter by remember { mutableStateOf<Tag?>(null) }

    LaunchedEffect(key1 = backdropScaffoldState.isRevealed) {
        backdropOpen = backdropScaffoldState.isRevealed
    }

    LaunchedEffect(key1 = backdropOpen, key2 = backdropTrigger)
    {
        if (backdropOpen) {
            backdropScaffoldState.reveal()
        } else {
            backdropScaffoldState.conceal()
        }
    }

    val setBackdropOpen: (Boolean) -> Unit = {
        backdropOpen = it
        backdropTrigger = !backdropTrigger
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
                navState = navState,
                numSelected = selectedRecordings.size,
                onOpenSearch = { navController.navigate("search") },
                onShareRecordings = { onShareRecordings(null) },
                onAddRecordingsToGroup = {
                    navController.navigate("selectGroup/" + "null")
                },
                onTag = {
                    navController.navigate("addTag/" + "null")
                },
                onDeleteRecordings = { onDeleteRecordings(null) },
                onClearSelected = onClearSelected,
                setBackdropOpen = setBackdropOpen,
                backdropOpen = backdropOpen
            )
        },
        frontLayerContent =
        {
            NavHost(navController = navController, startDestination = "recordingsScreen") {
                composable("recordingsScreen") {
                    RecordingsScreen(
                        recordings = recordings,
                        recordingsData = recordingsData,
                        groups = groups,
                        currentPlaybackRec = currentPlaybackRec,
                        onStartPlayback = onStartPlayback,
                        onOpenPlayback = { onSelectBackdrop(1); setBackdropOpen(true) },
                        onOpenRecordingInfo = { recording ->
                            navController.navigate("recordingInfo/" + ContentUris.parseId(recording.uri))
                        },
                        onClickTag = {
                            selectedSearchTagFilter = it
                            navController.navigate("search")
                        },
                        selectedRecordings = selectedRecordings,
                        onSelectRecording = onSelectRecording,
                        onCreateGroup = {
                            navController.navigate("newGroup")
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
                        onShare = {
                            onShareRecordings(recording!!)
                        },
                        onDelete = {
                            showDeleteDialog = true
                        },
                        onAddTag = {
                            recording?.also {
                                navController.navigate("addTag/" + ContentUris.parseId(it.uri))
                            }
                        },
                        onDeleteTag = {
                            onDeleteTag(recording!!, it)
                        },
                        onClickTag = {
                            selectedSearchTagFilter = it
                            navController.navigate("search")
                        },
                        onPlayTimestamp = { time ->
                            onStartPlayback(recording!!)
                            onSeekPlayback(time / recording.duration.toFloat())
                            setBackdropOpen(true)
                        },
                        onDeleteTimestamp = {
                            onDeleteTimestamp(recording!!, it)
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
                                            onDeleteRecordings(listOf(it))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        contentColor = MaterialTheme.colors.onBackground,
                                        backgroundColor = MaterialTheme.colors.onBackground
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
                                        contentColor = MaterialTheme.colors.onBackground,
                                        backgroundColor = MaterialTheme.colors.onBackground
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
                    val recording =
                        if (uri == "null") null else recordings.find { it.uri == Uri.parse(uri) }
                    val recordingData = if (recording == null) null else
                        recordingsData.find { it.recordingUri == recording.uri.toString() }

                    TagScreen(
                        recording = recording,
                        recordingData = recordingData,
                        tags = tags,
                        onAddTag = { rec, tag ->
                            onAddRecordingTag(rec, tag)
                            onClearSelected()
                            navController.navigateUp()
                        }
                    )
                }
                composable("search") {
                    SearchScreen(
                        recordings = recordings,
                        recordingsData = recordingsData,
                        groups = listOf(),
                        tagFilter = selectedSearchTagFilter,
                        onOpenRecordingInfo = { recording ->
                            navController.navigate("recordingInfo/" + ContentUris.parseId(recording.uri))
                        },
                        onStartPlayback = onStartPlayback,
                        onOpenPlayback = { onSelectBackdrop(1); setBackdropOpen(true) },
                        onOpenSelectTag = { navController.navigate("selectTag") },
                        onRemoveTagFilter = { selectedSearchTagFilter = null },
                        onOpenTimeStamp = { rec, timeStamp ->
                            onStartPlayback(rec)
                            onSeekPlayback(timeStamp.timeMilli / rec.duration.toFloat())
                            setBackdropOpen(true)
                        }
                    )
                }
                composable("selectTag") {
                    TagSelectScreen(tags = tags, onSelectTag = {
                        navController.navigateUp()
                        selectedSearchTagFilter = it
                    })
                }
                composable("addTimestamp/{uri}/{timeMilli}") { backStackEntry ->
                    val uri =
                        "content://media/external/audio/media/" + (backStackEntry.arguments?.getString(
                            "uri"
                        ) ?: "")
                    val recording = recordings.find { it.uri == Uri.parse(uri) }!!
                    val timeMilli = backStackEntry.arguments?.getString("timeMilli")?.toLong()!!
                    TimestampScreen(timeMilli = timeMilli, onComplete = { title, desc ->
                        onAddRecordingTimestamp(
                            recording,
                            timeMilli,
                            title,
                            desc
                        )
                        navController.navigateUp()
                        setBackdropOpen(true)
                    })
                }
                composable("newGroup") {
                    NewGroupScreen(
                        onCreate = { name, uri ->
                            onCreateGroup(name, uri)
                            navController.navigateUp()
                        },
                        onChooseImage = onChooseImage
                    )
                }
                composable("selectGroup/{uri}") { backStackEntry ->
                    val uri =
                        "content://media/external/audio/media/" + (backStackEntry.arguments?.getString(
                            "uri"
                        ) ?: "")
                    val recording =
                        if (uri == "null") null else recordings.find { it.uri == Uri.parse(uri) }

                    SelectGroupScreen(
                        groups = groups,
                        recordingsData = recordingsData,
                        onSelect = {
                            onAddRecordingsToGroup(it, recording)
                            onClearSelected()
                            navController.navigateUp()
                        },
                        onCreateGroup = {
                            navController.navigate("newGroup")
                        }
                    )
                }
            }
        },
        backLayerBackgroundColor = MaterialTheme.colors.surface,
        frontLayerScrimColor = MaterialTheme.colors.background.copy(alpha = .6f),
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
                elapsedRecordingTime = elapsedRecordingTime,
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
                },
                onAddRecordingTimestamp = { rec, time ->
                    navController.navigate("addTimestamp/" + ContentUris.parseId(rec.uri) + "/" + time.toString())
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
    playbackProgress: Long,
    elapsedRecordingTime: Long,
    currentPlaybackRec: Recording?,
    currentPlaybackRecData: RecordingData?,
    onPausePlayPlayback: () -> Unit,
    onSeekPlayback: (Float) -> Unit,
    onOpenRecordingInfo: (Recording) -> Unit,
    onAddRecordingTimestamp: (Recording, Long) -> Unit
) {
    Column {
        Spacer(Modifier.height(2.dp))
        Crossfade(
            targetState = selectedBackdrop,
            modifier = Modifier.animateContentSize()
        ) {
            if (it == 0) {
                RecordingBackdrop(
                    elapsedRecordingTime = elapsedRecordingTime,
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
                    onOpenRecordingInfo = onOpenRecordingInfo,
                    onAddRecordingTimestamp = onAddRecordingTimestamp
                )
            }
        }
        NewButtons(
            buttonPos = selectedBackdrop,
            onClick = onSelectBackdrop
        )
    }
}

@ExperimentalAnimationApi
@Composable
fun TopBar(
    navState: State<NavBackStackEntry?>,
    numSelected: Int,
    onOpenSearch: () -> Unit,
    onDeleteRecordings: () -> Unit,
    onShareRecordings: () -> Unit,
    onAddRecordingsToGroup: () -> Unit,
    onTag: () -> Unit,
    onClearSelected: () -> Unit,
    setBackdropOpen: (Boolean) -> Unit,
    backdropOpen: Boolean
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }
    val openBackdropButtonDegrees by animateFloatAsState(if (backdropOpen) 180f else 0f)

    TopAppBar(
        modifier = Modifier.height(71.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 0.dp,
        contentColor = MaterialTheme.colors.onSurface,
        title = {
            if (numSelected <= 0)
                Text(
                    "micCheck",
                    style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.ExtraBold)
                )
            else
                Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClearSelected) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Clear Selected"
                        )
                    }
                    Text(
                        "$numSelected Recording${if (numSelected > 1) "s" else ""}",
                        style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
        },
        actions = {
            Box {
                if (numSelected <= 0) {
                    Row(
                        Modifier
                            .align(Alignment.TopEnd)
                            .animateContentSize()
                    ) {
                        IconButton({ setBackdropOpen(!backdropOpen) }) {
                            Icon(
                                Icons.Rounded.ExpandMore,
                                contentDescription = "Expand Backdrop",
                                modifier = Modifier.rotate(openBackdropButtonDegrees)
                            )
                        }
                        AnimatedVisibility(
                            visible = (navState.value?.destination?.route == "recordingsScreen")
                        ) {
                            IconButton(onClick = { onOpenSearch(); setBackdropOpen(false) }) {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = "Search"
                                )
                            }
                        }
                        IconButton(onClick = {
                            moreMenuExpanded = true
                        }) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = "More Options"
                            )
                        }
                    }
                } else {
                    Row(
                        Modifier
                            .align(Alignment.TopEnd)
                            .animateContentSize()
                    ) {
                        IconButton({ onShareRecordings(); onClearSelected() }) {
                            Icon(Icons.Rounded.Share, "Share")
                        }
                        IconButton({ onAddRecordingsToGroup() }) {
                            Icon(Icons.Rounded.Inventory2, "Add to Group")
                        }
                        IconButton({ onTag() }) {
                            Icon(Icons.Rounded.Sell, "Group")
                        }
                        IconButton({ onDeleteRecordings(); onClearSelected() }) {
                            Icon(Icons.Rounded.Delete, "Delete")
                        }
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
                elapsedRecordingTime = 0L,
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
                onOpenRecordingInfo = { },
                onAddRecordingTimestamp = { _, _ -> }
            )
        }
    }
}
//
//@ExperimentalFoundationApi
//@ExperimentalMaterialApi
//@ExperimentalAnimationApi
//@Preview
//@Composable
//fun MainScreenPreview() {
//    var sel by remember {
//        mutableStateOf(0)
//    }
//    val onSelectScreen: (Int) -> Unit = {
//        sel = it
//    }
//    var selB by remember {
//        mutableStateOf(0)
//    }
//    val onSelectBackdrop: (Int) -> Unit = {
//        selB = it
//    }
//    var recording by remember {
//        mutableStateOf(RecordingState.WAITING)
//    }
//
//    MicCheckTheme {
//        Surface {
//            AppUI(
//                recordings = listOf(
//                    Recording(
//                        Uri.EMPTY, "Placeholder 1", 150000, 0, "0B",
//                        date = LocalDateTime.now().plusDays(1)
//                    ),
//                    Recording(Uri.parse("file:///tmp/android.txt"), "Placeholder 2", 0, 0, "0B"),
//                    Recording(Uri.parse("file:///tmp/android2.txt"), "Placeholder 3", 0, 0, "0B"),
//                ),
//                recordingsData = listOf(
//                    RecordingData(Uri.EMPTY.toString()),
//                    RecordingData(Uri.parse("file:///tmp/android.txt").toString()),
//                    RecordingData(Uri.parse("file:///tmp/android2.txt").toString()),
//                ),
//                tags = listOf(),
//                recordingState = recording,
//                currentPlaybackRec = null,
//                playbackState = PlaybackStateCompat.STATE_NONE,
//                playbackProgress = 0,
//                onStartRecord = { recording = RecordingState.RECORDING },
//                onPausePlayRecord = { },
//                onStopRecord = { recording = RecordingState.WAITING },
//                onFinishedRecording = { _, _ -> },
//                onStartPlayback = { /*TODO*/ },
//                onPausePlayPlayback = { /*TODO*/ },
//                onSeekPlayback = { },
//                onEditFinished = { _, _, _ -> },
//                onAddRecordingTag = { _, _ -> },
//                onDeleteRecording = { },
//                onSelectBackdrop = onSelectBackdrop,
//                selectedBackdrop = selB
//            )
//        }
//    }
//}