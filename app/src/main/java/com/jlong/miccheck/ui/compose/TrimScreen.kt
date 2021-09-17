package com.jlong.miccheck.ui.compose

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jlong.miccheck.Recording
import com.jlong.miccheck.ui.theme.MicCheckTheme
import java.time.LocalDateTime

@ExperimentalMaterialApi
@Composable
fun TrimScreen(
    recording: Recording,
    onPlaySelection: (Float, Recording) -> Unit,
    playbackProgress: Float,
    onStopSelection: () -> Unit,
    onTrim: (Recording, Long, Long, String) -> Unit,
    onExit: () -> Unit
) {
    var selection by remember { mutableStateOf(0f..1f) }
    var viewRange by remember {
        mutableStateOf(0f..1f)
    }
    val (titleText, setTitleText) = remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    var isPlayingSection by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = playbackProgress >= selection.endInclusive) {
        if (isPlayingSection && playbackProgress >= selection.endInclusive) {
            if (selection.endInclusive != 1f)
                onStopSelection()
            isPlayingSection = false
        }
    }

    Surface(
        color = MaterialTheme.colors.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            Text(
                "Trim Recording",
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                "${recording.name} - ${recording.duration.toLong().toTimestamp()}",
                style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextField(
                value = titleText,
                onValueChange = setTitleText,
                modifier = Modifier
                    .fillMaxWidth(),
                placeholder = {
                    Text(
                        "Name of Clip",
                        style = MaterialTheme.typography.h6
                    )
                },
                colors =
                TextFieldDefaults.textFieldColors(
                    backgroundColor =
                    if (isSystemInDarkTheme()) MaterialTheme.colors.secondary
                    else
                        MaterialTheme.colors.surface.copy(
                            alpha = .65f
                        ),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colors.onSurface
                ),
                shape = RoundedCornerShape(14.dp),
                textStyle = MaterialTheme.typography.h6,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text((selection.start * recording.duration).toLong().toTimestamp())
                Text((selection.endInclusive * recording.duration).toLong().toTimestamp())
            }

            RangeSlider(
                values = selection,
                onValueChange = { selection = it },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colors.primary,
                    activeTrackColor = MaterialTheme.colors.primary,
                    inactiveTrackColor = MaterialTheme.colors.secondary
                ),
                valueRange = viewRange
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text((viewRange.start * recording.duration).toLong().toTimestamp())
                TextButton(onClick = { viewRange = selection }) {
                    Text("Zoom to Selection")
                }
                Text((viewRange.endInclusive * recording.duration).toLong().toTimestamp())
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    enabled = viewRange != 0f..1f,
                    onClick = {
                        viewRange = 0f..1f
                    }
                ) {
                    Text("Reset Zoom")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    isPlayingSection = if (isPlayingSection) {
                        onStopSelection()
                        false
                    } else {
                        onPlaySelection(selection.start, recording)
                        true
                    }
                }) {
                    Crossfade(
                        targetState = isPlayingSection,
                        modifier = Modifier.animateContentSize()
                    ) {
                        if (it)
                            Text("Pause Selection")
                        else
                            Text("Play Selection")
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        if (titleText.isNotBlank()) {
                            onTrim(
                                recording,
                                (recording.duration * selection.start).toLong(),
                                (recording.duration * selection.endInclusive).toLong(),
                                titleText
                            )
                            onExit()
                        }
                    }
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@ExperimentalMaterialApi
@Preview
@Composable
fun TrimScreenPreview() {
    MicCheckTheme {
        TrimScreen(
            recording = Recording(
                Uri.EMPTY,
                "New Recording",
                60000,
                0,
                "0",
                LocalDateTime.now(),
                ""
            ),
            { _, _ -> },
            0f,
            {},
            { _, _, _, _ -> },
            {}
        )
    }
}