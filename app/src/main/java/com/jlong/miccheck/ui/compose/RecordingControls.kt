package com.jlong.miccheck.ui.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
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
    val (titleText, setTitleText) = remember { mutableStateOf("") }
    val (descText, setDescText) = remember { mutableStateOf("") }
    var blinkVal by remember { mutableStateOf(true) }
    val blinkAlpha by animateFloatAsState(
        targetValue = when (blinkVal) {
            true -> 1f
            false -> 0f
        },
        finishedListener = {
            blinkVal = if (recordingState == RecordingState.RECORDING)
                !blinkVal
            else
                true
        },
        animationSpec = tween(1000, 250)
    )

    LaunchedEffect(key1 = recordingState) {
        if (recordingState == RecordingState.RECORDING)
            blinkVal = !blinkVal
    }

    Column(
        Modifier
            .padding(12.dp, 0.dp, 12.dp, 18.dp)
            .animateContentSize()
            .fillMaxWidth()
    ) {
        AnimatedVisibility(visible = (recordingState != RecordingState.STOPPED)) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface (
                        shape = CircleShape,
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colors.onSurface.copy(
                            alpha = blinkAlpha
                        )
                    ) {}
                    Spacer(Modifier.width(8.dp))
                    Text(
                        elapsedRecordingTime.toTimestamp(),
                        style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
               AnimatedVisibility(
                   visible = (recordingState == RecordingState.WAITING),
                   modifier =
                   Modifier
                       .align(Alignment.CenterHorizontally)
                       .padding(top = 12.dp)
               ) {
                    Text(
                        "Ready to record...",
                        style = MaterialTheme.typography.h6.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colors.onSurface.copy(alpha = .5f),
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
        AnimatedVisibility(visible = recordingState == RecordingState.STOPPED) {
            Column {
                MetadataOptions(titleText, setTitleText, descText, setDescText)
            }
        }
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            RecordingButtons(
                recordingState = recordingState,
                onStartRecord = onStartRecord,
                onPausePlayRecord = onPausePlayRecord,
                onStopRecord = onStopRecord,
                onFinishedRecording = {
                    if (titleText.isNotBlank())
                    {
                        onFinishedRecording(titleText, descText)
                        setTitleText("")
                        setDescText("")
                    }
                },
                onCancel = onCancel
            )
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun MetadataOptions(
    titleText: String,
    setTitleText: (String) -> Unit,
    descText: String,
    setDescText: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column {
        TextField(
            value = titleText,
            setTitleText,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Recording Title") },
            colors =
            TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.secondary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colors.onSurface,

                ),
            shape = RoundedCornerShape(32.dp),
            textStyle = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.SemiBold),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
        Spacer(Modifier.height(12.dp))
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
            shape = RoundedCornerShape(32.dp),
            textStyle = MaterialTheme.typography.body1,
        )

        Spacer(Modifier.height(20.dp))
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
                recordingState = RecordingState.RECORDING
            )
        }
    }
}