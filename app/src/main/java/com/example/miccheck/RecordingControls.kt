package com.example.miccheck

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.miccheck.ui.theme.MicCheckTheme

@ExperimentalFoundationApi
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
            .fillMaxWidth()
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
                cursorColor = Color.Black,

                ),
            shape = RoundedCornerShape(40),
            textStyle = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.SemiBold),
        )
        AnimatedVisibility(visible = recordingState == RecordingState.PAUSED || recordingState == RecordingState.STOPPED) {
            Column {
                Spacer(Modifier.height(12.dp))
                MetadataOptions(descText, setDescText)
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            RecordingButtons(
                recordingState = recordingState,
                onStartRecord = onStartRecord,
                onPausePlayRecord = onPausePlayRecord,
                onStopRecord = onStopRecord,
                onFinishedRecording = { onFinishedRecording(titleText, descText) },
                onCancel = onCancel
            )
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun MetadataOptions(
    descText: String,
    setDescText: (String) -> Unit
) {
    Column {
        TextField(
            value = descText,
            setDescText,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(.25f),
            placeholder = { Text("Description") },
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
}

@ExperimentalAnimationApi
@Composable
fun RecordingButtons(
    recordingState: RecordingState,
    onStartRecord: () -> Unit,
    onPausePlayRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onFinishedRecording: () -> Unit,
    onCancel: () -> Unit
) {
    Row(modifier = Modifier.animateContentSize()) {
        Row(Modifier.padding(0.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
            LargeButton(
                onClick =
                when (recordingState) {
                    RecordingState.WAITING -> onStartRecord
                    RecordingState.RECORDING, RecordingState.PAUSED -> onStopRecord
                    RecordingState.STOPPED -> onFinishedRecording
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

@ExperimentalFoundationApi
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