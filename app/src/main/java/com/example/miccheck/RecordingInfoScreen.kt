package com.example.miccheck

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.miccheck.ui.theme.MicCheckTheme
import com.google.accompanist.flowlayout.FlowRow
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
            backgroundColor = MaterialTheme.colors.primary.copy(
                alpha = .65f
            ),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Color.Black
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
            ?: "Not really sure how you got here, but we have no clue what recording you're referencing.\nMaybe try again?"
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
            backgroundColor = MaterialTheme.colors.primary.copy(
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

@Composable
fun RecordingsInfoScreen(
    recording: Recording?,
    recordingData: RecordingData?,
    onPlay: () -> Unit,
    onEditFinished: (String, String) -> Unit,
    onDelete: () -> Unit,
    onAddTag: () -> Unit
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
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp, 0.dp, 8.dp, 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                            ) + "\n" +
                            (
                                    if (((recording.duration / 1000) / 60) / 60 > 0)
                                        (((recording.duration / 1000) / 360).toString() + ":")
                                    else
                                        ""
                                    ) + //hours
                            ((recording.duration / 1000) / 60) % 60 + ":" + //minutes
                            ((recording.duration / 1000) % 60).toString()
                            + " • " + recording.sizeStr, //seconds,
                    style = MaterialTheme.typography.h6,
                )

                Row {
                    IconButton(onPlay) {
                        Icon(Icons.Default.PlayArrow, "Play")
                    }
                    IconButton({
                        editing = if (editing) {
                            onEditFinished(titleText, descText); false
                        } else true
                    }) {
                        Crossfade(targetState = editing) {
                            if (it)
                                Icon(Icons.Default.Save, "Save")
                            else
                                Icon(Icons.Default.Edit, "Edit")
                        }
                    }
                    IconButton(onDelete) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            LazyRow(horizontalArrangement = Arrangement.Center) {
                item {
                    Spacer(modifier = Modifier.width(18.dp))
                    Chip(
                        text = "",
                        onClick = onAddTag,
                        color = MaterialTheme.colors.primary.copy(
                            alpha = .65f
                        ),
                        icon = Icons.Default.Add,
                        contentColor = MaterialTheme.colors.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Chip(
                        text = recordingData?.group?.name ?: "No Group",
                        onClick = { },
                        color = MaterialTheme.colors.primary.copy(
                            alpha = .65f
                        ),
                        contentColor = MaterialTheme.colors.onPrimary
                    )
                }
                itemsIndexed(recordingData!!.tags) { _, tag ->
                    Row {
                        Spacer(modifier = Modifier.width(8.dp))
                        Chip(tag.name, onClick = { })
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
        Crossfade(targetState = editing, modifier = Modifier.animateContentSize()) {
            if (it) RecordingInfoDescriptionField(descriptionText = descText, setText = setDescText)
            else RecordingInfoDescriptionText(description = recordingData?.description)
        }
    }


}

@Composable
fun TagScreen(
    recording: Recording?,
    recordingData: RecordingData?,
    tags: List<Tag>,
    onAddTag: (Recording, Tag) -> Unit,
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
                    backgroundColor = MaterialTheme.colors.primary.copy(
                        alpha = .65f
                    ),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                textStyle = MaterialTheme.typography.h6,
            )
            Spacer(Modifier.width(12.dp))
            IconButton(
                onClick = {
                    recording?.also {
                        onAddTag(it, Tag(text))
                    }
                },
            ) {
                Icon(Icons.Default.Done, "Done")
            }
        }
        FlowRow(Modifier.fillMaxWidth(), mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
            tags.forEach {
                if (recordingData?.tags?.find { tag -> tag.name == it.name } == null)
                    Chip(
                        text = it.name,
                        onClick = {
                            recording?.also { rec ->
                                onAddTag(rec, it)
                            }
                        }
                    )
            }
        }
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

@Preview
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
                            "vestibulum. Maecenas ipsum lacus, lacinia quis posuere ut, pulvinar vitae dolor.\n" +
                            "Integer eu nibh at nisi ullamcorper sagittis id vel leo. Integer feugiat \n" +
                            "faucibus libero, at maximus nisl suscipit posuere. Morbi nec enim nunc. \n" +
                            "Phasellus bibendum turpis ut ipsum egestas, sed sollicitudin elit convallis. \n" +
                            "Cras pharetra mi tristique sapien vestibulum lobortis. Nam eget bibendum metus, \n" +
                            "non dictum mauris. Nulla at tellus sagittis, viverra est a, bibendum metus."
                ),
                onEditFinished = { _, _ -> },
                onPlay = { },
                onDelete = { },
                onAddTag = { }
            )
        }
    }
}