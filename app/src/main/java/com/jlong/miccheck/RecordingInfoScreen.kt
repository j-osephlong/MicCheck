package com.jlong.miccheck

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.jlong.miccheck.ui.theme.MicCheckTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.random.Random

@Composable
fun RecordingInfoTitleText(
    title: String?,
) {
    Text(
        title ?: "EMPTY CASSETTE",
        style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.ExtraBold),
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(18.dp, 0.dp, 18.dp, 12.dp)
    )
}

@Composable
fun RecordingInfoTitleField(
    titleText: String,
    setText: (String) -> Unit
) {
    TextField(
        value = titleText,
        setText,
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp, 0.dp, 18.dp, 12.dp),
        colors =
        TextFieldDefaults.textFieldColors(
            backgroundColor = MaterialTheme.colors.secondary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colors.onSurface
        ),
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.ExtraBold),
    )
}

@Composable
fun RecordingInfoDescriptionText(
    description: String?
) {
    Text(
        (description
            ?: "Not really sure how you got here, but we have no clue what recording is supposed to be here.\nMaybe try again?"
                ).ifEmpty { "No description written yet." },
        modifier = Modifier.padding(18.dp, 0.dp, 18.dp, 18.dp)
    )
}

@Composable
fun RecordingInfoDescriptionField(
    descriptionText: String,
    setText: (String) -> Unit
) {
    TextField(
        value = descriptionText,
        setText,
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp, 0.dp, 18.dp, 18.dp),
        placeholder = { Text("Description") },
        colors =
        TextFieldDefaults.textFieldColors(
            backgroundColor = MaterialTheme.colors.secondary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colors.onSurface
        ),
        singleLine = false,
        shape = RoundedCornerShape(14.dp)
    )
}

@ExperimentalAnimationApi
@Composable
fun RecordingsInfoScreen(
    recording: Recording?,
    recordingData: RecordingData?,
    onPlay: () -> Unit,
    onPlayTimestamp: (Long) -> Unit,
    onEditFinished: (String, String) -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onAddTag: () -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onClickTag: (Tag) -> Unit,
    onDeleteTimestamp: (TimeStamp) -> Unit
) {
    val (titleText, setTitleText) = remember { mutableStateOf(recording?.name ?: "") }
    val (descText, setDescText) = remember { mutableStateOf(recordingData?.description ?: "") }
    var editing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(18.dp))
        Crossfade(targetState = editing, modifier = Modifier.animateContentSize()) {
            if (it) RecordingInfoTitleField(titleText = titleText, setText = setTitleText)
            else RecordingInfoTitleText(title = recording?.name)
        }
        if (recording != null) {
            Text(
                recording.date.format(
                    DateTimeFormatter.ofPattern("LLLL d")
                        .withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault())
                ) +
                        when (recording.date.dayOfMonth % 10) {
                            1 -> if (recording.date.dayOfMonth == 11) "th" else "st"
                            2 -> if (recording.date.dayOfMonth == 12) "th" else "nd"
                            3 -> if (recording.date.dayOfMonth == 13) "th" else "rd"
                            else -> "th"
                        } +
                        recording.date.format(
                            DateTimeFormatter.ofPattern(", yyyy")
                                .withLocale(Locale.getDefault())
                                .withZone(ZoneId.systemDefault())
                        ) + " • " +
                        recording.duration.toLong().toTimestamp()
                        + " • " + recording.sizeStr, //seconds,
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(start = 18.dp)
            )
            Spacer(Modifier.height(18.dp))
        }
        LazyColumn {
            if (recording != null)
                item {
                    LazyRow(horizontalArrangement = Arrangement.Center) {
                        item {
                            Spacer(modifier = Modifier.width(18.dp))
                            Chip(
                                text = "",
                                onClick = onAddTag,
                                color = MaterialTheme.colors.secondary,
                                icon = Icons.Rounded.Add,
                                contentColor = MaterialTheme.colors.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Chip(
                                text = recordingData?.group?.name ?: "No Group",
                                onClick = { },
                                color = MaterialTheme.colors.secondary,
                                contentColor = MaterialTheme.colors.onSurface
                            )
                        }
                        itemsIndexed(recordingData!!.tags) { _, tag ->
                            Row {
                                Spacer(modifier = Modifier.width(8.dp))
                                Chip(
                                    tag.name,
                                    onClick = { onClickTag(tag) },
                                    icon =
                                    if (editing) Icons.Rounded.Remove
                                    else null,
                                    onIconClick = {
                                        onDeleteTag(tag)
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.padding(start = 8.dp)) {
                        IconButton(onPlay) {
                            Icon(Icons.Rounded.PlayArrow, "Play")
                        }
                        IconButton({
                            editing = if (editing) {
                                onEditFinished(titleText, descText); false
                            } else true
                        }) {
                            Crossfade(targetState = editing) {
                                if (it)
                                    Icon(Icons.Rounded.Save, "Save")
                                else
                                    Icon(Icons.Rounded.Edit, "Edit")
                            }
                        }
                        IconButton(onShare) {
                            Icon(Icons.Rounded.Share, "Share")
                        }
                        IconButton(onDelete) {
                            Icon(Icons.Rounded.Delete, "Delete")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            item {
                Text(
                    "Description",
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(18.dp, 0.dp, 0.dp, 0.dp)
                )
                Spacer(Modifier.height(8.dp))
                Crossfade(
                    targetState = editing, modifier = Modifier
                        .animateContentSize()
//                        .padding(start = 30.dp)
                ) {
                    if (it) RecordingInfoDescriptionField(
                        descriptionText = descText,
                        setText = setDescText
                    )
                    else RecordingInfoDescriptionText(description = recordingData?.description)
                }
                Spacer(Modifier.height(8.dp))
            }

            if (recordingData?.timeStamps?.isNotEmpty() == true) {
                item {
                    Text(
                        "Time Stamps",
                        style = MaterialTheme.typography.h5,
                        modifier = Modifier.padding(start = 18.dp)
                    )
                }
                itemsIndexed(recordingData.timeStamps.sortedBy { it.timeMilli }) { index, timeStamp ->
                    Column(
                        Modifier
                            .padding(start = 18.dp)
                    ) {
                        TimestampElm(
                            timeStamp,
                            { onPlayTimestamp(timeStamp.timeMilli) },
                            { onDeleteTimestamp(timeStamp) }
                        )
                        if (index != recordingData.timeStamps.size - 1)
                            Divider(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@ExperimentalAnimationApi
@Composable
private fun TimestampElm(
    timeStamp: TimeStamp,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDescription by remember { mutableStateOf(false) }
    val descriptionButtonDegrees by animateFloatAsState(
        targetValue = if (showDescription) 180f else 0f
    )
    Column(
        Modifier
            .animateContentSize()
            .clickable { onClick() }) {
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    timeStamp.timeMilli.toTimestamp(),
                    style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.ExtraBold)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    timeStamp.name,
                    style = MaterialTheme.typography.body1
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (timeStamp.description != null)
                    IconButton(
                        onClick = { showDescription = !showDescription },
                    ) {
                        Icon(
                            Icons.Rounded.ExpandMore, "Show/Hide Description",
                            modifier = Modifier.rotate(descriptionButtonDegrees)
                        )
                    }
                IconButton(onClick = { onDelete() }, modifier = Modifier.padding(end = 8.dp)) {
                    Icon(Icons.Rounded.Close, "Remove")
                }
            }
        }
        if (timeStamp.description != null) {
            Spacer(Modifier.height(4.dp))
            AnimatedVisibility(visible = showDescription) {
                Text(timeStamp.description!!)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun TagScreen(
    recording: Recording?,
    recordingData: RecordingData?,
    tags: List<Tag>,
    onAddTag: (Recording?, Tag) -> Unit,
) {
    val (text, setText) = remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Text(
            "Add Tag",
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.ExtraBold),
            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 12.dp)
        )
        Text(
            "Create a tag or select one from below.",
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 18.dp)
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 0.dp, 0.dp, 18.dp)
        ) {
            TextField(
                value = text,
                onValueChange = setText,
                modifier = Modifier
                    .weight(1f),
                placeholder = { Text("Tag name", style = MaterialTheme.typography.h6) },
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
            )
            Spacer(Modifier.width(12.dp))
            IconButton(
                onClick = {
                    onAddTag(recording, Tag(text))
                },
            ) {
                Icon(Icons.Rounded.Done, "Done")
            }
        }
        FlowRow(Modifier.fillMaxWidth(), mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
            tags.forEach {
                if (recordingData?.tags?.find { tag -> tag.name == it.name } == null)
                    Chip(
                        text = it.name,
                        onClick = {
                            onAddTag(recording, it)
                        }
                    )
            }
        }
    }
}

@Composable
fun TimestampScreen(
    timeMilli: Long,
    onComplete: (String, String?) -> Unit
) {
    val (title, setTitle) = remember { mutableStateOf("") }
    val (description, setDescription) = remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Text(
            "Add Timestamp",
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.ExtraBold),
            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 4.dp)
        )
        Text(
            "@" + (
                    if (((timeMilli / 1000) / 60) / 60 > 0)
                        (((timeMilli / 1000) / 360).toString() + ":")
                    else
                        ""
                    ) + //hours
                    ((timeMilli / 1000) / 60) % 60 + ":" + //minutes
                    ((timeMilli / 1000) % 60).toString(),
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 12.dp)
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = title,
                onValueChange = setTitle,
                modifier = Modifier
                    .weight(1f),
                placeholder = {
                    Text(
                        "Add a title.",
                        style = MaterialTheme.typography.h6
                    )
                },
                colors =
                TextFieldDefaults.textFieldColors(
                    backgroundColor = MaterialTheme.colors.surface.copy(
                        alpha = .65f
                    ),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                textStyle = MaterialTheme.typography.h6,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                maxLines = 1
            )
            Spacer(Modifier.width(12.dp))
            IconButton(
                onClick = {
                    if (title.isNotBlank())
                        onComplete(
                            title,
                            if (description.isNotBlank()) description else null
                        )
                },
            ) {
                Icon(Icons.Rounded.Done, "Done")
            }
        }
        Spacer(Modifier.height(12.dp))
        TextField(
            value = description,
            setDescription,
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            placeholder = { Text("(Optional) Add a short description.") },
            colors =
            TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.surface.copy(
                    alpha = .65f
                ),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color.Black
            ),
            singleLine = false,
            shape = RoundedCornerShape(14.dp)
        )
    }

}

@Composable
fun TagSelectScreen(
    tags: List<Tag>,
    onSelectTag: (Tag) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Text(
            "Select Tag",
            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.ExtraBold),
            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 12.dp)
        )
        Text(
            "Select a tag from below.",
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 18.dp)
        )
        FlowRow(Modifier.fillMaxWidth(), mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
            tags.forEach {
                Chip(
                    text = it.name,
                    onClick = {
                        onSelectTag(it)
                    }
                )
            }
        }
    }
}

//@Preview
@Composable
fun TagScreenPreview(

) {
    Surface(Modifier.fillMaxSize()) {
        MicCheckTheme {
            TagScreen(
                tags = listOf<Tag>().apply {
                    repeat(30) {
                        plus(
                            listOf(
                                Tag(
                                    "hahawowlookathim go".substring(Random.nextInt(12))
                                )
                            )
                        )
                    }
                },
                recording = Recording(
                    Uri.EMPTY,
                    "",
                    0,
                    0,
                    "0B"
                ),
                recordingData = RecordingData(
                    Uri.EMPTY.toString()
                ),
                onAddTag = { _, _ -> }
            )
        }
    }
}

@ExperimentalAnimationApi
@Preview
@Composable
fun InfoPreview() {
    Surface(Modifier.fillMaxSize()) {
        MicCheckTheme {
            RecordingsInfoScreen(
                recording = Recording(
                    Uri.EMPTY,
                    "New Recording",
                    90000,
                    0,
                    "0B"
                ),
                recordingData = RecordingData(
                    Uri.EMPTY.toString(),
                    listOf(Tag("Tag"), Tag("TagTag"), Tag("Tag")),
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque nisl eros, \n" +
                            "pulvinar facilisis justo mollis, auctor consequat urna. Morbi a bibendum metus. \n" +
                            "Donec scelerisque sollicitudin enim eu venenatis. Duis tincidunt laoreet ex, \n" +
                            "in pretium orci vestibulum eget. Class aptent taciti sociosqu ad litora torquent\n" +
                            "per conubia nostra, per inceptos himenaeos. Duis pharetra luctus lacus ut \n" +
                            "vestibulum. Maecenas ipsum lacus, lacinia quis posuere ut, pulvinar vitae dolor.\n",
                    timeStamps = listOf(
//                        TimeStamp(13000L, "Sucks", "Wowowo\nwowowowo\nh"),
//                        TimeStamp(240000L, "Haha  woww owow owo "),
//                        TimeStamp(240000L, ""),
                    )

                ),
                onEditFinished = { _, _ -> },
                onPlay = { },
                onDelete = { },
                onAddTag = { },
                onDeleteTag = { },
                onClickTag = { },
                onPlayTimestamp = { _ -> },
                onDeleteTimestamp = { },
                onShare = { }
            )
        }
    }
}