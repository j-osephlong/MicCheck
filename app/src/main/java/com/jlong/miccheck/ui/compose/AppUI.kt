package com.jlong.miccheck.ui.compose

import android.content.ContentUris
import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.jlong.miccheck.*
import com.jlong.miccheck.ui.theme.MicCheckTheme
import kotlinx.coroutines.launch

enum class NavDest(val route: String) {
    RecordingsScreen("recordingsScreen") {
        /**
         * @param arg No arguments
         */
        override fun routeTo(vararg arg: Any?) = "recordingsScreen"
    },
    RecordingInfoScreen("recordingInfo/{uri}") {
        /**
         * @param arg Recording [Uri]
         */
        override fun routeTo(vararg arg: Any?) =
            "recordingInfo/${ContentUris.parseId(arg[0] as Uri)}"
    },
    AddTagScreen("addTag/{uri}") {
        /**
         * @param arg Recording [Uri?]
         */
        override fun routeTo(vararg arg: Any?) =
            "addTag/${(arg[0] as Uri?)?.let { ContentUris.parseId(it) } ?: "null"}"
    },
    SearchScreen("search") {
        /**
         * @param arg No arguments
         */
        override fun routeTo(vararg arg: Any? /*No arguments*/) = "search"
    },
    SelectTagScreen("selectTag") {
        /**
         * @param arg No arguments
         */
        override fun routeTo(vararg arg: Any? /*No arguments*/) = "selectTag"
    },
    AddTimestampScreen("addTimestamp/{uri}/{timeMilli}") {
        /**
         * @param arg (1) Recording [Uri] (2) Timestamp Milli [Long]
         */
        override fun routeTo(vararg arg: Any? /*Must provide Uri and Long*/) =
            "addTimestamp/${ContentUris.parseId(arg[0] as Uri)}/${arg[1] as Long}"
    },
    NewGroupScreen("newGroup") {
        override fun routeTo(vararg arg: Any? /*No arguments*/) = "newGroup"
    },
    SelectGroupScreen("selectGroup/{uri}") {
        override fun routeTo(vararg arg: Any? /*Must be a uri*/) =
            "selectGroup/${(arg[0] as Uri?)?.let { ContentUris.parseId(it) } ?: "null"}"
    },
    GroupScreen("group/{uuid}") {
        override fun routeTo(vararg arg: Any? /*Must be a uuid string*/) =
            "group/${arg[0] as String}"
    },
    SelectRecordingsScreen("selectRecordings/{uuid}") {
        override fun routeTo(vararg arg: Any? /*Must be a uuid string*/) =
            "selectRecordings/${arg[0] as String}"
    },
    TrimScreen("trimRecording/{uri}") {
        override fun routeTo(vararg arg: Any? /*Must be a uri*/) =
            "trimRecording/${ContentUris.parseId(arg[0] as Uri)}"
    };

    abstract fun routeTo(vararg arg: Any?): String
}

@ExperimentalPagerApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun AppUI(
    viewModel: AppViewModel,
    onStartRecord: () -> Unit,
    onPausePlayRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onStartPlayback: (Recording) -> Unit,
    onStartGroupPlayback: (Int, RecordingGroup) -> Unit,
    onPausePlayPlayback: () -> Unit,
    onStopPlayback: () -> Unit,
    onSeekPlayback: (Float) -> Unit,
    onSkipPlayback: (Long) -> Unit,
    onShareRecordings: (Recording?) -> Unit,
    onChooseImage: ((Uri) -> Unit) -> Unit,
    showDatePicker: ((Long) -> Unit) -> Unit,
    onTrim: (Recording, Long, Long, String) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit
) {
    val context = LocalContext.current

    val navController = rememberAnimatedNavController()
    val navState = navController.currentBackStackEntryAsState()
    var backdropOpen by remember { mutableStateOf(false) }
    var backdropTrigger by remember { mutableStateOf(false) }
    val backdropScaffoldState =
        rememberBackdropScaffoldState(
            initialValue = BackdropValue.Concealed,
        )
    var selectedSearchTagFilter by remember { mutableStateOf<Tag?>(null) }
    val recordingsScreenPager = rememberPagerState(pageCount = 2)
    var showPlaybackSpeedDialog by remember { mutableStateOf(false) }

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
        peekHeight = 66.dp,
        frontLayerElevation = 8.dp,
        frontLayerShape = RoundedCornerShape(22.dp, 0.dp, 0.dp, 0.dp),
        frontLayerBackgroundColor = MaterialTheme.colors.background,
        appBar = {
            TopBar(
                navState = navState,
                selectedRecordingsScreen = recordingsScreenPager.currentPage,
                numSelected = viewModel.selectedRecordings.size,
                onOpenSearch = { navController.navigate("search") },
                onShareRecordings = { onShareRecordings(null) },
                onCreateGroup = {
                    navController.navigate(NavDest.NewGroupScreen.routeTo())
                },
                onAddRecordingsToGroup = {
                    navController.navigate(NavDest.SelectGroupScreen.routeTo(null))
                },
                onTag = {
                    navController.navigate(NavDest.AddTagScreen.routeTo(null))
                },
                onDeleteRecordings = { viewModel.onDeleteRecordings(context) },
                onClearSelected = { viewModel.selectedRecordings.removeAll(viewModel.selectedRecordings) },
                setBackdropOpen = setBackdropOpen,
                backdropOpen = backdropOpen,
                onShowPlaybackSpeedDialog = { showPlaybackSpeedDialog = true }
            )
        },
        frontLayerContent =
        {
            AnimatedNavHost(
                navController = navController,
                startDestination = NavDest.RecordingsScreen.route,
                enterTransition = { _, _ ->
                    slideInHorizontally(initialOffsetX = { 2000 })
                },
                exitTransition = { _, _ ->
                    slideOutHorizontally(targetOffsetX = { -2000 })
                },
                popEnterTransition = { _, _ ->
                    slideInHorizontally(initialOffsetX = { -2000 })
                }
            ) {
                composable(NavDest.RecordingsScreen.route) {
                    RecordingsScreen(
                        pageState = recordingsScreenPager,
                        recordings = viewModel.recordings,
                        recordingsData = viewModel.recordingsData,
                        groups = viewModel.groups,
                        currentPlaybackRec = viewModel.currentPlaybackRec,
                        selectedRecordings = viewModel.selectedRecordings,
                        onSelectRecording = viewModel::onSelectRecording,

                        onStartPlayback = onStartPlayback,
                        onOpenPlayback = { viewModel.onSetBackdrop(1); setBackdropOpen(true) },
                        onOpenRecord = { viewModel.onSetBackdrop(0); setBackdropOpen(true) },
                        onOpenRecordingInfo = { recording ->
                            navController.navigate(NavDest.RecordingInfoScreen.routeTo(recording.uri))
                        },
                        onClickTag = {
                            selectedSearchTagFilter = it
                            navController.navigate(NavDest.SearchScreen.routeTo())
                        },
                        onClearSelected = { viewModel.selectedRecordings.removeAll(viewModel.selectedRecordings) },
                        onOpenGroup = { group ->
                            navController.navigate(NavDest.GroupScreen.routeTo(group.uuid))
                        },
                        showDatePicker = showDatePicker,
                        onCreateGroup = {
                            navController.navigate(NavDest.NewGroupScreen.routeTo())
                        }
                    )
                }
                composable(NavDest.RecordingInfoScreen.route) { backStackEntry ->
                    val uri =
                        "content://media/external/audio/media/" + (backStackEntry.arguments?.getString(
                            "uri"
                        ) ?: "")
                    val recording = viewModel.recordings.find { it.uri == Uri.parse(uri) }
                    val recordingData =
                        viewModel.recordingsData.find { it.recordingUri == recording?.uri.toString() }
                    val recordingGroup =
                        viewModel.groups.find {
                            it.uuid == recordingData?.groupUUID
                        }
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    RecordingsInfoScreen(
                        recording = recording,
                        recordingData = recordingData,
                        recordingGroup = recordingGroup,
                        clipParent = viewModel.recordings.find { it.uri.toString() == recordingData?.clipParentUri },
                        onEditFinished = { title, desc ->
                            recording?.also {
                                viewModel.onEditRecordingDataFinished(
                                    context, it, title, desc
                                )
                            }
                        },
                        onPlay = {
                            recording?.also {
                                onStartPlayback(it)
                                viewModel.onSetBackdrop(1)
                                setBackdropOpen(true)
                            }
                        },
                        onShare = {
                            onShareRecordings(recording!!)
                        },
                        onDelete = {
                            showDeleteDialog = true
                        },
                        onTrim = {
                            navController.navigate(NavDest.TrimScreen.routeTo(recording!!.uri))
                        },
                        onAddTag = {
                            recording?.also {
                                navController.navigate(NavDest.AddTagScreen.routeTo(recording.uri))
                            }
                        },
                        onDeleteTag = {
                            viewModel.onDeleteTag(recording!!, it)
                        },
                        onClickTag = {
                            selectedSearchTagFilter = it
                            navController.navigate(NavDest.SearchScreen.routeTo())
                        },
                        onClickGroupTag = {
                            if (recordingGroup != null)
                                navController.navigate(NavDest.GroupScreen.routeTo(recordingGroup.uuid))
                        },
                        onPlayTimestamp = { time ->
                            onStartPlayback(recording!!)
                            onSeekPlayback(time / recording.duration.toFloat())
                            setBackdropOpen(true)
                        },
                        onDeleteTimestamp = {
                            viewModel.onDeleteTimestamp(recording!!, it)
                        },
                        onOpenClipParent = {
                            if (recordingData?.clipParentUri != null)
                                navController.navigate(
                                    NavDest.RecordingInfoScreen.routeTo(
                                        Uri.parse(
                                            recordingData.clipParentUri
                                        )
                                    )
                                )
                        }
                    )

                    ConfirmDialog(
                        title = "Delete recording?",
                        extraText = "Deleting is permanent and cannot be undone.",
                        actionName = "Delete",
                        visible = showDeleteDialog,
                        onClose = { showDeleteDialog = false }
                    ) {
                        showDeleteDialog = false
                        navController.navigateUp()
                        recording?.also {
                            viewModel.onDeleteRecordings(context, listOf(it))
                        }
                    }
                }
                composable(NavDest.AddTagScreen.route) { backStackEntry ->
                    val uri =
                        "content://media/external/audio/media/" + (backStackEntry.arguments?.getString(
                            "uri"
                        ) ?: "")
                    val recording =
                        if (uri == "null") null else viewModel.recordings.find {
                            it.uri == Uri.parse(
                                uri
                            )
                        }
                    val recordingData = if (recording == null) null else
                        viewModel.recordingsData.find { it.recordingUri == recording.uri.toString() }

                    TagScreen(
                        onAddTag = { rec, tag ->
                            viewModel.onAddTagToRecording(rec, tag)
                            viewModel.selectedRecordings.removeAll(viewModel.selectedRecordings)
                            navController.navigateUp()
                        },
                        tags = viewModel.tags,
                        recording = recording,
                        recordingData = recordingData,
                    )
                }
                composable(NavDest.SearchScreen.route) {
                    SearchScreen(
                        recordings = viewModel.recordings,
                        recordingsData = viewModel.recordingsData,
                        groups = viewModel.groups,
                        tagFilter = selectedSearchTagFilter,
                        onOpenRecordingInfo = { recording ->
                            navController.navigate(NavDest.RecordingInfoScreen.routeTo(recording.uri))
                        },
                        onStartPlayback = onStartPlayback,
                        onOpenPlayback = { viewModel.onSetBackdrop(1); setBackdropOpen(true) },
                        onOpenSelectTag = { navController.navigate(NavDest.SelectTagScreen.routeTo()) },
                        onRemoveTagFilter = { selectedSearchTagFilter = null },
                        onOpenTimeStamp = { rec, timeStamp ->
                            onStartPlayback(rec)
                            onSeekPlayback(timeStamp.timeMilli / rec.duration.toFloat())
                            setBackdropOpen(true)
                        },
                        onOpenGroup = { group ->
                            navController.navigate(NavDest.GroupScreen.routeTo(group.uuid))
                        }
                    )
                }
                composable(NavDest.SelectTagScreen.route) {
                    TagSelectScreen(
                        tags = viewModel.tags,
                        onSelectTag = {
                            navController.navigateUp()
                            selectedSearchTagFilter = it
                        })
                }
                composable(NavDest.AddTimestampScreen.route) { backStackEntry ->
                    val uri =
                        "content://media/external/audio/media/" + (backStackEntry.arguments?.getString(
                            "uri"
                        ) ?: "")
                    val recording = viewModel.recordings.find { it.uri == Uri.parse(uri) }!!
                    val timeMilli = backStackEntry.arguments?.getString("timeMilli")?.toLong()!!
                    TimestampScreen(timeMilli = timeMilli, onComplete = { title, desc ->
                        viewModel.onAddTimestampToRecording(
                            recording,
                            timeMilli,
                            title,
                            desc
                        )
                        navController.navigateUp()
                        setBackdropOpen(true)
                    })
                }
                composable(NavDest.NewGroupScreen.route) {
                    NewGroupScreen(
                        onCreate = { name, uri, color ->
                            if (name.isNotEmpty()) {
                                viewModel.onCreateGroup(name, uri, color)
                                navController.navigateUp()
                            }
                        },
                        onChooseImage = onChooseImage
                    )
                }
                composable(NavDest.SelectGroupScreen.route) { backStackEntry ->
                    val uri =
                        "content://media/external/audio/media/" + (backStackEntry.arguments?.getString(
                            "uri"
                        ) ?: "")
                    val recording =
                        if (uri == "null") null else viewModel.recordings.find {
                            it.uri == Uri.parse(
                                uri
                            )
                        }

                    SelectGroupScreen(
                        groups = viewModel.groups,
                        recordingsData = viewModel.recordingsData,
                        onSelect = {
                            viewModel.onAddRecordingsToGroup(context, it, recording)
                            viewModel.selectedRecordings.removeAll(viewModel.selectedRecordings)
                            navController.navigateUp()
                            navController.navigate(NavDest.GroupScreen.routeTo(it.uuid))
                        },
                        recordings = viewModel.recordings
                    )
                }
                composable(NavDest.GroupScreen.route) { backStackEntry ->
                    val uuid =
                        (backStackEntry.arguments?.getString(
                            "uuid"
                        ) ?: "")
                    val group = viewModel.groups.find { it.uuid == uuid }
                    if (group == null) {
                        navController.navigate(NavDest.RecordingsScreen.routeTo())
                        return@composable
                    }
                    val recData = viewModel.recordingsData.filter { it.groupUUID == uuid }
                    val recs = viewModel.recordings.filter { rec ->
                        viewModel.recordingsData.find { it.recordingUri == rec.uri.toString() }!!.groupUUID == uuid
                    }
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    GroupScreen(
                        group = group,
                        groupRecordings = recs.sortedBy { rec -> recData.find { it.recordingUri == rec.uri.toString() }!!.groupOrderNumber },
                        groupRecordingsData = recData,
                        onOpenRecordingInfo = { recording ->
                            navController.navigate(NavDest.RecordingInfoScreen.routeTo(recording.uri))
                        },
                        onPlayRecording = { rec ->
                            onStartGroupPlayback(recData.find {it.recordingUri == rec.uri.toString()}!!.groupOrderNumber, group)
                            viewModel.onSetBackdrop(1)
                            setBackdropOpen(true)
                        },
                        currentlyPlayingRec = viewModel.currentPlaybackRec,
                        onClickTag = {
                            selectedSearchTagFilter = it
                            navController.navigate(NavDest.SearchScreen.routeTo())
                        },
                        onDeleteGroup = {
                            showDeleteDialog = true
                        },
                        onAddRecordings = {
                            navController.navigate(NavDest.SelectRecordingsScreen.routeTo(group.uuid))
                        },
                        onChooseImage = onChooseImage,
                        onSaveEdit = viewModel::onEditGroup,
                        onRemoveRecording = viewModel::onRemoveRecordingFromGroup
                    )

                    ConfirmDialog(
                        title = "Delete group?",
                        extraText = "This will not delete any recordings.\nDeleting is permanent and cannot be undone.",
                        actionName = "Delete",
                        visible = showDeleteDialog,
                        onClose = { showDeleteDialog = false }
                    ) {
                        showDeleteDialog = false
                        navController.navigateUp()
                        viewModel.onDeleteGroup(group)
                    }
                }
                composable(NavDest.SelectRecordingsScreen.route) { backStackEntry ->
                    val uuid =
                        (backStackEntry.arguments?.getString(
                            "uuid"
                        ) ?: "")
                    val group = viewModel.groups.find { it.uuid == uuid }!!

                    val filteredRecData = viewModel.recordingsData
                    //.filter {it.groupUUID != uuid}
                    val filteredRecordings = viewModel.recordings
                    //.filter {rec -> filteredRecData.find {it.recordingUri == rec.uri.toString()} != null }
                    SelectRecordingsScreen(
                        recordings = filteredRecordings,
                        recordingsData = filteredRecData,
                        groups = viewModel.groups,
                        onDone = {
                            navController.navigateUp()
                            it.forEach { rec ->
                                viewModel.onAddRecordingsToGroup(context, group, rec)
                            }
                        }
                    )
                }
                composable(NavDest.TrimScreen.route) { backStackEntry ->
                    val uri =
                        "content://media/external/audio/media/" + (backStackEntry.arguments?.getString(
                            "uri"
                        ) ?: "")
                    val recording =
                        viewModel.recordings.find { it.uri == Uri.parse(uri) } ?: Recording(
                            Uri.EMPTY,
                            "What a weird bug",
                            0,
                            0,
                            "0",
                            path = ""
                        )
                    TrimScreen(
                        recording = recording,
                        onPlaySelection = { start, rec ->
                            onStartPlayback(rec)
                            onPausePlayPlayback()
                            onSeekPlayback(start)
                            onPausePlayPlayback()
                        },
                        playbackProgress = viewModel.playbackProgress / recording.duration.toFloat(),
                        onStopSelection = onStopPlayback,
                        onTrim = onTrim,
                        onExit = {
                            navController.navigateUp()
                        }
                    )
                }
            }
        },
        backLayerBackgroundColor = MaterialTheme.colors.surface,
        frontLayerScrimColor = MaterialTheme.colors.background.copy(alpha = .6f),
        backLayerContent = {
            Backdrop(
                selectedBackdrop = viewModel.selectedBackdrop,
                onSelectBackdrop = viewModel::onSetBackdrop,
                onStartRecord = onStartRecord,
                onPausePlayRecord = onPausePlayRecord,
                onStopRecord = onStopRecord,
                onFinishedRecording =
                { title, desc ->
                    viewModel.onRecordingFinished(context, title, desc)
                    setBackdropOpen(false)
                },
                onCancel = { setBackdropOpen(false) },
                recordingState = viewModel.recordingState,
                playbackState = viewModel.currentPlaybackState,
                playbackProgress = viewModel.playbackProgress,
                elapsedRecordingTime = viewModel.recordTime,
                currentPlaybackRec = viewModel.currentPlaybackRec,
                currentPlaybackRecData =
                viewModel.recordingsData.find { recData ->
                    recData.recordingUri == viewModel.currentPlaybackRec?.uri.toString()
                },
                currentPlaybackGroup =
                viewModel.groups.find { group ->
                    group.uuid ==
                        viewModel.recordingsData.find { recData ->
                            recData.recordingUri == viewModel.currentPlaybackRec?.uri.toString()
                        }?.groupUUID
                },
                onPausePlayPlayback = onPausePlayPlayback,
                onSeekPlayback = onSeekPlayback,
                onSkipPlayback = onSkipPlayback,
                onOpenRecordingInfo = { recording ->
                    navController.navigate(NavDest.RecordingInfoScreen.routeTo(recording.uri))
                    setBackdropOpen(false)
                },
                onAddRecordingTimestamp = { rec, time ->
                    navController.navigate(NavDest.AddTimestampScreen.routeTo(rec.uri, time))
                    setBackdropOpen(false)
                },
                isGroupPlayback = viewModel.isGroupPlayback
            )
        }
    )

    PlaybackSpeedDialog(
        visible = showPlaybackSpeedDialog,
        onExit = { showPlaybackSpeedDialog = false },
        onPositive = { showPlaybackSpeedDialog = false; onSetPlaybackSpeed(it) }
    )
}

@ExperimentalPagerApi
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
    currentPlaybackGroup: RecordingGroup?,
    onPausePlayPlayback: () -> Unit,
    onSeekPlayback: (Float) -> Unit,
    onSkipPlayback: (Long) -> Unit,
    isGroupPlayback: Boolean,
    onOpenRecordingInfo: (Recording) -> Unit,
    onAddRecordingTimestamp: (Recording, Long) -> Unit
) {
    val pageState = rememberPagerState(pageCount = 2)
    val coroutine = rememberCoroutineScope()
    val setBackdrop: (Int) -> Unit = {
        if (recordingState != RecordingState.STOPPED) {
            coroutine.launch {
                pageState.animateScrollToPage(it)
            }
            onSelectBackdrop(it)
        }
    }
    LaunchedEffect(key1 = selectedBackdrop) {
        setBackdrop(selectedBackdrop)
    }

    Column {
        Spacer(Modifier.height(2.dp))
        HorizontalPager(
            state = pageState,
            dragEnabled = false,
            modifier = Modifier.animateContentSize(),
        ) { backdrop ->
            if (backdrop == 0) {
                RecordingBackdrop(
                    elapsedRecordingTime = elapsedRecordingTime,
                    onStartRecord = onStartRecord,
                    onPausePlayRecord = onPausePlayRecord,
                    onStopRecord = onStopRecord,
                    onFinishedRecording = onFinishedRecording,
                    onCancel = onCancel,
                    recordingState = recordingState
                )
            } else if (backdrop == 1) {
                PlaybackBackdrop(
                    playbackState = playbackState,
                    playbackProgress = playbackProgress,
                    currentPlaybackRec = currentPlaybackRec,
                    currentPlaybackGroup = currentPlaybackGroup,
                    onPausePlayPlayback = onPausePlayPlayback,
                    onSeekPlayback = onSeekPlayback,
                    onOpenRecordingInfo = onOpenRecordingInfo,
                    onAddRecordingTimestamp = onAddRecordingTimestamp,
                    onSkipPlayback = onSkipPlayback,
                    isGroupPlayback = isGroupPlayback
                )
            }
        }
        NewButtons(
            buttonPos = pageState.currentPage,
            onClick = setBackdrop
        )
    }
}

@ExperimentalAnimationApi
@Composable
fun TopBar(
    navState: State<NavBackStackEntry?>,
    selectedRecordingsScreen: Int,
    numSelected: Int,
    onOpenSearch: () -> Unit,
    onDeleteRecordings: () -> Unit,
    onShareRecordings: () -> Unit,
    onCreateGroup: () -> Unit,
    onAddRecordingsToGroup: () -> Unit,
    onTag: () -> Unit,
    onClearSelected: () -> Unit,
    onShowPlaybackSpeedDialog: () -> Unit,
    setBackdropOpen: (Boolean) -> Unit,
    backdropOpen: Boolean
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val openBackdropButtonDegrees by animateFloatAsState(if (backdropOpen) 180f else 0f)

    val clearIcon: @Composable (() -> Unit)? = if (numSelected > 0) {
        {
            IconButton(onClearSelected) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Clear"
                )
            }
        }
    } else null

    TopAppBar(
        modifier = Modifier.height(65.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 0.dp,
        contentColor = MaterialTheme.colors.onSurface,
        navigationIcon = clearIcon,
        title = {
            if (numSelected <= 0)
                Text(
                    "micCheck",
                    style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.ExtraBold)
                )
            else
                Text(
                    "$numSelected",
                    style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.ExtraBold)
                )
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
                        AnimatedVisibility(visible = (selectedRecordingsScreen == 1 &&
                                navState.value?.destination?.route == "recordingsScreen")) {
                            IconButton(onClick = onCreateGroup) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = "New Group"
                                )
                            }
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
                                contentDescription = "Options"
                            )
                            DropdownMenu(
                                expanded = moreMenuExpanded,
                                onDismissRequest = { moreMenuExpanded = false },
                            ) {
                                DropdownMenuItem(onClick = {
                                    moreMenuExpanded = false
                                    onShowPlaybackSpeedDialog()
                                }) {
                                    Text("Playback speed")
                                }
                                DropdownMenuItem(onClick = {
                                    moreMenuExpanded = false
                                }) {
                                    Text("About")
                                }
                            }
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
                            Icon(Icons.Rounded.Inventory2, "Group")
                        }
                        IconButton({ onTag() }) {
                            Icon(Icons.Rounded.Sell, "Tag")
                        }
                        IconButton({ showDeleteDialog = true }) {
                            Icon(Icons.Rounded.Delete, "Delete")
                        }
                    }
                }
            }
        }
    )

    ConfirmDialog(
        title = "Delete $numSelected recording${if (numSelected > 1) "s" else ""}?",
        extraText = "Deleting is permanent and cannot be undone.",
        actionName = "Delete",
        visible = showDeleteDialog,
        onClose = { showDeleteDialog = false }
    ) {
        onDeleteRecordings()
        onClearSelected()
        showDeleteDialog = false
    }

    BackHandler(numSelected > 0) {
        onClearSelected()
    }
}

@ExperimentalPagerApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Preview
@Composable
fun BackdropPreview() {
    MicCheckTheme {
        Surface {
            Backdrop(
                selectedBackdrop = 0,
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
                    1,
                    0,
                    "0B",
                    path = ""
                ),
                currentPlaybackRecData = RecordingData(
                    Uri.EMPTY.toString(),
                    listOf(),
                    ""
                ),
                currentPlaybackGroup = null,
                onPausePlayPlayback = { /*TODO*/ },
                onSeekPlayback = { },
                onOpenRecordingInfo = { },
                onAddRecordingTimestamp = { _, _ -> },
                onSkipPlayback = {},
                isGroupPlayback = false
            )
        }
    }
}

//@ExperimentalAnimationApi
//@ExperimentalMaterialApi
//@ExperimentalFoundationApi
//@ExperimentalPagerApi
//@Preview
//@Composable
//fun AppUIPreview() {
//    MicCheckTheme() {
//        AppUI(
//            viewModel = AppViewModel(),
//            onStartRecord = { /*TODO*/ },
//            onPausePlayRecord = { /*TODO*/ },
//            onStopRecord = { /*TODO*/ },
//            onStartPlayback = {},
//            onStartGroupPlayback = {_, _, ->},
//            onPausePlayPlayback = { /*TODO*/ },
//            onStopPlayback = { /*TODO*/ },
//            onSeekPlayback = { _ ->},
//            onSkipPlayback = {},
//            onShareRecordings = {},
//            onChooseImage = {},
//            showDatePicker = {},
//            onTrim = {_, _, _, _->}
//        )
//    }
//}