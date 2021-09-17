package com.jlong.miccheck.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.jlong.miccheck.ui.theme.MicCheckTheme
import kotlinx.coroutines.launch

@ExperimentalPagerApi
@Composable
fun FirstLaunchScreen(
    onDone: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = 4)
    val coroutine = rememberCoroutineScope()

    val onNext: (Int) -> Unit = {
        coroutine.launch {
            pagerState.animateScrollToPage(it)
        }
    }
    Surface(
        Modifier
            .fillMaxSize()
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> TitlePage { onNext(1) }
                1 -> InfoPageOne { onNext(2) }
                2 -> InfoPageTwo { onNext(3) }
                3 -> ExitTourPage { onDone() }
            }
        }
    }
}

@Composable
fun TitlePage(onNext: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.Start) {
            Text(
                "micCheck",
                style = MaterialTheme.typography.h3.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontSize = MaterialTheme.typography.h5.fontSize)) {
                        append("Modern Audio Recorder and Organizer")
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append(" for You")
                        }
                    }
                }
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(
                    contentColor = MaterialTheme.colors.onSurface,
                    backgroundColor = MaterialTheme.colors.surface
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Take the tour",
                        style = TextStyle(fontSize = 18.sp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Rounded.ArrowForward, null)
                }
            }
        }

    }
}

@Composable
fun InfoPageOne(onNext: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.Start) {
            Text(
                buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontFamily = MaterialTheme.typography.h4.fontFamily,
                            fontSize = MaterialTheme.typography.h4.fontSize,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append("Record, Listen,")
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
                            append(" Simple.")
                        }
                    }
                }
            )
            Text(
                "micCheck lets you record and playback your audio with ease. " +
                        "Just swipe down on the top of the screen at any point to " +
                        "record or control playback.",
                style = MaterialTheme.typography.h6
            )
        }

        OutlinedButton(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(
                contentColor = MaterialTheme.colors.onSurface,
                backgroundColor = MaterialTheme.colors.surface
            ),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Next",
                    style = TextStyle(fontSize = 18.sp)
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.ArrowForward, null)
            }
        }
    }
}

@Composable
fun InfoPageTwo(onNext: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.Start) {
            Text(
                buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontFamily = MaterialTheme.typography.h4.fontFamily,
                            fontSize = MaterialTheme.typography.h4.fontSize,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append("Stay")
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.primary)) {
                            append(" Organized.")
                        }
                    }
                }
            )
            Text(
                "micCheck is packed with powerful features to help keep your recordings organized.",
                style = MaterialTheme.typography.h6
            )
            Spacer(Modifier.height(12.dp))
            listOf(
                Pair("Tags", "Tag your recordings to make searching and staying on topic easier."),
                Pair("Groups", "Keep your recordings in Groups, which look and feel like albums."),
                Pair("Timestamps", "Now you can organize the contents of your recordings, too."),
                Pair(
                    "Clips",
                    "Crop your recordings into sections, each acting as it's own recording, with the trim button."
                )
            ).forEachIndexed { index, str ->
                Column {
                    Text(
                        buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    fontFamily = MaterialTheme.typography.h4.fontFamily,
                                    fontSize = MaterialTheme.typography.h4.fontSize,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) {
                                append((index + 1).toString())
                            }
                            withStyle(
                                style = SpanStyle(
                                    fontFamily = MaterialTheme.typography.h6.fontFamily,
                                    fontSize = MaterialTheme.typography.h6.fontSize,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) {
                                append(" ${str.first}\n")
                            }
                            withStyle(
                                style = SpanStyle(
                                    fontFamily = MaterialTheme.typography.h6.fontFamily,
                                    fontSize = MaterialTheme.typography.h6.fontSize
                                )
                            ) {
                                append(str.second)
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            Text("Etc.", style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.SemiBold))
        }

        OutlinedButton(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(
                contentColor = MaterialTheme.colors.onSurface,
                backgroundColor = MaterialTheme.colors.surface
            ),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Next",
                    style = TextStyle(fontSize = 18.sp)
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.ArrowForward, null)
            }
        }
    }
}

@Composable
fun ExitTourPage(onDone: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.Start) {
            Text(
                "Thanks for downloading the app.",
                style = MaterialTheme.typography.h4.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontSize = MaterialTheme.typography.h5.fontSize)) {
                        append("We hope you enjoy it")
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append(" :)")
                        }
                    }
                }
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(
                    contentColor = MaterialTheme.colors.onSurface,
                    backgroundColor = MaterialTheme.colors.surface
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Done",
                        style = TextStyle(fontSize = 18.sp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Rounded.ArrowForward, null)
                }
            }
        }
    }
}

@ExperimentalPagerApi
@Preview
@Composable
fun FirstLaunchScreenPreview() {
    MicCheckTheme(true) {
        FirstLaunchScreen {

        }
    }
}