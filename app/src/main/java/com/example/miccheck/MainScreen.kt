package com.example.miccheck

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
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

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun MainScreen(
    recordings: List<Recording>,
    currentlyRecording: Boolean,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onSelectRecording: (Uri) -> Unit,
    onAddRecordingTag: (Uri) -> Unit,
    onSelectScreen: (Int) -> Unit,
    selectedScreen: Int,
) {
    Scaffold(
        topBar = { TopBar() },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text =
                { Text(if (!currentlyRecording) "Record" else "Stop Recording") },
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
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.animateContentSize()
            )
        },
    ) {
        Column (Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface {
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
            }

            when (selectedScreen) {
                else ->
                    RecordingsList(
                        recordings = recordings,
                        onSelectRecording = { /*TODO*/ },
                        onAddRecordingTag = onAddRecordingTag
                    )
            }
        }
    }
}

@Composable
fun TopBar () {
    var moreMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = Modifier.height(60.dp),
        backgroundColor = MaterialTheme.colors.background,
        contentColor = MaterialTheme.colors.onBackground,
        elevation = 0.dp,
        title = { Text(
            "micCheck",
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.ExtraBold)) },
        actions = {
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
                        )
                    ),
                    Recording(Uri.EMPTY, "Placeholder 2", 0, 0),
                    Recording(Uri.EMPTY, "Placeholder 3", 0, 0),
                ),
                currentlyRecording = recording,
                onStartRecord = { recording = true },
                onStopRecord = { recording = false },
                onSelectRecording = { /*TODO*/ },
                onAddRecordingTag = { /*TODO*/ },
                onSelectScreen = onSelectScreen,
                selectedScreen = sel
            )
        }
    }
}