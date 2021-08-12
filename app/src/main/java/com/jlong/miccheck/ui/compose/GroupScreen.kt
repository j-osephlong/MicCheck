package com.jlong.miccheck.ui.compose

import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.ImagePainter
import coil.compose.rememberImagePainter
import com.jlong.miccheck.Recording
import com.jlong.miccheck.RecordingData
import com.jlong.miccheck.RecordingGroup
import com.jlong.miccheck.ui.theme.MicCheckTheme
import java.time.LocalDateTime
import java.util.*

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun GroupScreen(
    group: RecordingGroup,
    recordings: List<Recording>,
    recordingsData: List<RecordingData>,
    onOpenRecordingInfo: (Recording) -> Unit,
    onPlayRecording: (Recording) -> Unit
) {
    val painter: ImagePainter? = if (group.imgUri == null || group.imgUri == "null")
        null
    else
        group.imgUri?.let {
            rememberImagePainter(data = Uri.parse(it),
                builder = {
                    crossfade(true)
                }
            )
        }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        LazyColumn(Modifier.fillMaxSize()) {
            //Image box
            item {
                BoxWithConstraints {
                    Surface(
                        shape = RoundedCornerShape(22.dp, 0.dp, 22.dp, 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(maxWidth),
                        color = MaterialTheme.colors.background,
                        onClick = { /*TODO:Editing*/ }
                    ) {
                        if (painter != null) Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.TopStart),
                            contentScale = ContentScale.Crop
                        ) else Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(group.fallbackColor),
                                            MaterialTheme.colors.primary
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(
                                            Float.POSITIVE_INFINITY,
                                            Float.POSITIVE_INFINITY
                                        )
                                    )
                                )
                        ) {}
                    }
                }
            }
            stickyHeader {
                Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colors.background) {
                    Column {
                        Spacer(Modifier.height(18.dp))
                        Text(
                            group.name,
                            style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.ExtraBold),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(18.dp, 0.dp)
                        )
                        Text(
                            "${recordings.size} Recording${if (recordings.size != 1) "s" else ""}"
                                    + " - ${
                                recordings.sumOf { it.duration }.toLong().toTimestamp()
                            }",
                            style = MaterialTheme.typography.h6.copy(
                                MaterialTheme.colors.onBackground.copy(
                                    .75f
                                )
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(18.dp, 0.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
            itemsIndexed(recordings) { index, item ->
                Column {
                    RecordingElm(
                        Pair(
                            item,
                            recordingsData.find { it.recordingUri == item.uri.toString() }!!
                        ),
                        onOpenRecordingInfo = onOpenRecordingInfo,
                        onClick = { onPlayRecording(item) },
                        onClickTag = { /*TODO:Usability*/ },
                        isSelectable = false,
                        isSelected = false,
                        onSelect = { /*NOT USED*/ }
                    )

                    if (index != recordings.size - 1)
                        Divider(Modifier.padding(18.dp, 0.dp, 0.dp, 0.dp))
                }
            }
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Preview
@Composable
fun GroupScreenPreview() {
    MicCheckTheme {
        Surface(Modifier.fillMaxSize()) {
            GroupScreen(group = RecordingGroup(
                "The Fall of Hobo Johnson",
                null,
                Color.Red.toArgb(),
                UUID.randomUUID().toString()
            ),
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
                ), {}, {}
            )
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun GroupsList(
    recordingsData: List<RecordingData>,
    groups: List<RecordingGroup>,
    onClickGroup: (RecordingGroup) -> Unit,
    onCreateGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(18.dp, 0.dp)
        ) {
            item {
                Row {
                    NewGroupButton(
                        Modifier.weight(.5f),
                        onCreateGroup
                    )
                    Spacer(Modifier.width(18.dp))
                    if (groups.isNotEmpty())
                        GroupElm(
                            modifier = Modifier.weight(.5f),
                            group = groups[0],
                            numRecordings = recordingsData.filter { it.group == groups[0] }.size,
                            { onClickGroup(groups[0]) }
                        )
                    else
                        Spacer(Modifier.weight(.5f))
                }
                Spacer(Modifier.height(18.dp))
            }
            if (groups.size > 1)
                items(groups.subList(1, groups.size).chunked(2)) { groupPair ->
                    Column {
                        Row {
                            GroupElm(
                                group = groupPair[0],
                                numRecordings = recordingsData.filter { it.group == groupPair[0] }.size,
                                modifier = Modifier.weight(.5f),
                                onClick = { onClickGroup(groupPair[0]) }
                            )
                            Spacer(Modifier.width(18.dp))
                            if (groupPair.size > 1) {
                                GroupElm(
                                    group = groupPair[1],
                                    numRecordings = recordingsData.filter { it.group == groupPair[1] }.size,
                                    modifier = Modifier.weight(.5f),
                                    onClick = { onClickGroup(groupPair[1]) }
                                )
                                Spacer(Modifier.height(18.dp))
                            } else {
                                Spacer(Modifier.height(18.dp))
                                Spacer(Modifier.weight(.5f))
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                }
        }
    }
}

@Composable
fun NewGroupButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(modifier = modifier, color = MaterialTheme.colors.background) {
        Column(Modifier.fillMaxWidth()) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(18.dp), modifier = Modifier
                        .fillMaxWidth()
                        .height(maxWidth),
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                    contentPadding = PaddingValues(0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colors.surface)
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            null,
                            Modifier
                                .size(36.dp)
                                .align(Alignment.Center),
                            tint = MaterialTheme.colors.primary
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Create Group",
                style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.SemiBold)
            )

        }
    }
}

@ExperimentalMaterialApi
@Composable
fun GroupElm(
    modifier: Modifier = Modifier,
    group: RecordingGroup,
    numRecordings: Int,
    onClick: () -> Unit
) {
    val painter: ImagePainter? = if (group.imgUri == null || group.imgUri == "null")
        null
    else
        group.imgUri?.let {
            rememberImagePainter(data = Uri.parse(it),
                builder = {
                    crossfade(true)
                }
            )
        }
    Surface(modifier = modifier, color = MaterialTheme.colors.background) {
        Column(Modifier.fillMaxWidth()) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(maxWidth),
                    color = MaterialTheme.colors.background,
                    onClick = onClick
                ) {
                    if (painter != null) Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.TopStart),
                        contentScale = ContentScale.Crop
                    ) else Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(group.fallbackColor),
                                        MaterialTheme.colors.primary
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(
                                        Float.POSITIVE_INFINITY,
                                        Float.POSITIVE_INFINITY
                                    )
                                )
                            )
                    ) {}
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                group.name,
                style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                "$numRecordings Recording${if (numRecordings != 1) "s" else ""}",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onBackground.copy(alpha = .7f)
            )
        }

    }
}

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Preview
@Composable
fun GroupElmPreview() {
    val list = listOf(
        RecordingGroup(
            "New Group A", uuid = UUID.randomUUID().toString()
        ),
        RecordingGroup(
            "New Group B", uuid = UUID.randomUUID().toString()
        ),
        RecordingGroup(
            "New Group C", uuid = UUID.randomUUID().toString()
        ),
    )
    Surface(Modifier.fillMaxSize()) {
        MicCheckTheme {
            GroupsList(
                recordingsData = listOf(),
                groups = list, onClickGroup = {}, {})
        }
    }
}

@ExperimentalMaterialApi
@Composable
fun NewGroupScreen(
    onCreate: (String, Uri?) -> Unit,
    onChooseImage: ((Uri) -> Unit) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val (text, setText) = remember { mutableStateOf("") }
    var painter: Painter? = null
    var imgUri: Uri? by remember { mutableStateOf(null) }
    val chooseImageCallback: (Uri) -> Unit = {
        imgUri = it
    }

    if (imgUri != null)
        painter = rememberImagePainter(data = imgUri)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Column(
            Modifier
                .fillMaxSize()
        ) {
            Spacer(Modifier.height(18.dp))
            Text(
                "Create Group",
                style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.padding(start = 18.dp)
            )
            Text(
                "Give your group a name and an image.\n" +
                        "You can add recordings to your group from the recordings screen.",
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(18.dp, 0.dp)
            )
            Spacer(Modifier.height(18.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth(.5f)
                ) {
                    if (painter != null)
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .size(maxWidth),
                            color = MaterialTheme.colors.background,
                            onClick = { onChooseImage(chooseImageCallback) }
                        ) {
                            Image(
                                painter = painter,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .align(Alignment.TopStart),
                                contentScale = ContentScale.Crop
                            )
                        }
                    else
                        OutlinedButton(
                            onClick = { onChooseImage(chooseImageCallback) },
                            shape = RoundedCornerShape(18.dp), modifier = Modifier
                                .size(maxWidth),
                            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                            contentPadding = PaddingValues(0.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colors.surface)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    null,
                                    Modifier
                                        .size(36.dp)
                                        .align(Alignment.Center),
                                    tint = MaterialTheme.colors.primary
                                )
                            }
                        }
                }
            }
            Spacer(Modifier.height(18.dp))
            TextField(
                value = text,
                onValueChange = setText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp, 0.dp, 18.dp, 0.dp),
                placeholder = {
                    Text(
                        "Group name",
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
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                LargeButton(onClick = { onCreate(text, imgUri) }) {
                    Icon(Icons.Rounded.Done, "Done")
                }
            }
        }
    }
}

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun SelectGroupScreen(
    groups: List<RecordingGroup>,
    recordingsData: List<RecordingData>,
    onSelect: (RecordingGroup) -> Unit,
    onCreateGroup: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(18.dp))
            Text(
                "Select Group",
                style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.padding(start = 18.dp)
            )
            Text(
                "Select a group to add your recording(s) to.",
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(18.dp, 0.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            GroupsList(
                recordingsData = recordingsData,
                groups = groups,
                onClickGroup = onSelect,
                onCreateGroup = onCreateGroup
            )
        }
    }
}
