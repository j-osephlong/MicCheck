package com.jlong.miccheck.ui.compose

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jlong.miccheck.RecordingState
import com.jlong.miccheck.ui.theme.MicCheckTheme

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun RecordingBackdrop(
    elapsedRecordingTime: Long,
    onStartRecord: () -> Unit,
    onPausePlayRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onFinishedRecording: (String, String) -> Unit,
    onCancel: () -> Unit,
    recordingState: RecordingState
) {
    val (titleText, setTitleText) = remember { mutableStateOf("New Recording") }
    val (descText, setDescText) = remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        Modifier
            .padding(12.dp, 0.dp, 12.dp, 18.dp)
            .animateContentSize()
            .fillMaxWidth()
    ) {
        AnimatedVisibility(visible = (recordingState != RecordingState.WAITING)) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        elapsedRecordingTime.toTimestamp(),
                        style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
                Spacer(Modifier.height(20.dp))
            }
        }
        TextField(
            value = titleText,
            setTitleText,
            modifier = Modifier.fillMaxWidth(),
            colors =
            TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.secondary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colors.onSurface,

                ),
            shape = RoundedCornerShape(40),
            textStyle = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.SemiBold),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
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
                onFinishedRecording = {
                    onFinishedRecording(titleText, descText)
                    setTitleText("New Recording")
                    setDescText("")
                },
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
                backgroundColor = MaterialTheme.colors.secondary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colors.onSurface
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
    val focusManager = LocalFocusManager.current

    Row(modifier = Modifier.animateContentSize()) {
        Row(Modifier.padding(0.dp, 0.dp), verticalAlignment = Alignment.CenterVertically) {
            LargeButton(
                onClick =
                {
                    when (recordingState) {
                        RecordingState.WAITING -> {
                            onStartRecord(); focusManager.clearFocus()
                        }
                        RecordingState.RECORDING, RecordingState.PAUSED -> {
                            onStopRecord(); focusManager.clearFocus()
                        }
                        RecordingState.STOPPED -> {
                            onFinishedRecording(); focusManager.clearFocus()
                        }
                    }
                }
            ) {
                Crossfade(targetState = recordingState) {
                    when (it) {
                        RecordingState.WAITING -> Icon(Icons.Rounded.Mic, "Record")
                        RecordingState.RECORDING, RecordingState.PAUSED -> Icon(
                            Icons.Rounded.Stop,
                            "Stop"
                        )
                        RecordingState.STOPPED -> Icon(Icons.Rounded.Check, "Done")
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
                        {
                            when (recordingState) {
                                RecordingState.WAITING -> {
                                    onCancel(); focusManager.clearFocus()
                                }
                                else -> {
                                    onPausePlayRecord(); focusManager.clearFocus()
                                }
                            }
                        }
                    ) {
                        Crossfade(targetState = recordingState) {
                            when (it) {
                                RecordingState.WAITING -> Icon(
                                    Icons.Rounded.Close,
                                    "Cancel"
                                )
                                RecordingState.RECORDING -> Icon(
                                    Icons.Rounded.Pause,
                                    "Pause"
                                )
                                else -> Icon(Icons.Rounded.Mic, "Continue Recording")
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
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colors.surface) {
            RecordingBackdrop(
                elapsedRecordingTime = 0L,
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