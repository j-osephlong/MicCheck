package com.jlong.miccheck.ui.compose

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.InsertPhoto
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import coil.compose.ImagePainter
import coil.compose.rememberImagePainter
import com.jlong.miccheck.*
import com.jlong.miccheck.ui.theme.MicCheckTheme
import java.time.LocalDateTime
import java.util.*

@ExperimentalMaterialApi
@Composable
fun GroupCard (
    group: RecordingGroup,
    onClick: () -> Unit,
    groupRecordings: List<Recording>
) {
    Card (
        backgroundColor = Color(group.fallbackColor),
        shape = RoundedCornerShape(18.dp),
        elevation = 0.dp,
        onClick = onClick
    ) {
        val textColor = if (Color(group.fallbackColor).luminance() > .65f) Color.Black else Color.White
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


            Column (Modifier.fillMaxWidth()) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp, 18.dp, 0.dp, 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(maxWidth / 5 * 2),
                        color = MaterialTheme.colors.background,
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
                                            Color(
                                                if (isSystemInDarkTheme())
                                                    ColorUtils.blendARGB(
                                                        group.fallbackColor,
                                                        Color.Black.toArgb(),
                                                        .15f
                                                    )
                                                else
                                                    ColorUtils.blendARGB(
                                                        group.fallbackColor,
                                                        Color.White.toArgb(),
                                                        .4f
                                                    )
                                            ),
                                            Color(group.fallbackColor)
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(
                                            Float.POSITIVE_INFINITY,
                                            Float.POSITIVE_INFINITY
                                        )
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Column {
                                groupRecordings.forEachIndexed { i, rec ->
                                    if (i > 2)
                                        return@forEachIndexed
                                    Text(
                                        rec.name,
                                        style = MaterialTheme.typography.h6.copy(fontStyle = FontStyle.Italic),
                                        color = textColor.copy(alpha = .7f),
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        group.name,
                        style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.SemiBold),
                        color = textColor
                    )

                    Text(
                        "${groupRecordings.size} Rec${if(groupRecordings.size!=1) "s" else ""}",
                        style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.SemiBold),
                        color = textColor.copy(alpha = .5f)
                    )

                }
            }
    }
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun GroupScreen(
    group: RecordingGroup,
    groupRecordings: List<Recording>,
    groupRecordingsData: List<RecordingData>,
    onOpenRecordingInfo: (Recording) -> Unit,
    onPlayRecording: (Recording) -> Unit,
    onClickTag: (Tag) -> Unit,
    onDeleteGroup: () -> Unit,
    onAddRecordings: () -> Unit,
    onChooseImage: ((Uri) -> Unit) -> Unit,
    onSaveEdit: (RecordingGroup, String, String?, Int?) -> Unit,
    onRemoveRecording: (RecordingGroup, Recording) -> Unit,
    currentlyPlayingRec: Recording?,

) {
    var imgUri by remember {
        mutableStateOf(group.imgUri)
    }
    val painter: ImagePainter? = if (imgUri == null || imgUri == "null")
        null
    else
        imgUri?.let {
            rememberImagePainter(data = Uri.parse(it),
                builder = {
                    crossfade(true)
                }
            )
        }
    var colorVal by remember {
        mutableStateOf(group.fallbackColor)
    }
    val color = animateColorAsState(targetValue = Color(colorVal))
    val chooseImageCallback: (Uri) -> Unit = {
        imgUri = it.toString()
    }
    val (text, setText) = remember { mutableStateOf(group.name)}
    var editing by remember { mutableStateOf(false) }
    val selectedRecordings = remember { mutableStateListOf<Recording>().also { it += groupRecordings } }
    val onSelect: (Recording) -> Unit = {
        if (selectedRecordings.contains(it))
            selectedRecordings.remove(it)
        else
            selectedRecordings.add(it)
    }


    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Box (Modifier.fillMaxSize()){
            LazyColumn(
                Modifier
                    .fillMaxSize().align(Alignment.TopStart)
            ) {
                //Image box
                item {
                    BoxWithConstraints (Modifier.animateContentSize()) {
                        Surface(
                            shape = RoundedCornerShape(22.dp, 0.dp, 22.dp, 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (!editing) maxWidth / 5 * 4 else 0.dp),
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
                                                Color(
                                                    if (isSystemInDarkTheme())
                                                        ColorUtils.blendARGB(
                                                            color.value.toArgb(),
                                                            Color.Black.toArgb(),
                                                            .3f
                                                        )
                                                    else
                                                        ColorUtils.blendARGB(
                                                            color.value.toArgb(),
                                                            Color.White.toArgb(),
                                                            .8f
                                                        )
                                                ),
                                                color.value
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
                            Crossfade (editing, modifier = Modifier.animateContentSize()) {
                                if (!it)
                                    Text(
                                        group.name,
                                        style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.ExtraBold),
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(18.dp, 0.dp)
                                    )
                                else
                                    TitleField(titleText = text, setText = setText, color = color.value)
                            }
                            Text(
                                "${groupRecordings.size} Recording${if (groupRecordings.size != 1) "s" else ""}"
                                        + " - ${
                                    groupRecordings.sumOf { it.duration }.toLong().toTimestamp()
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
                itemsIndexed(groupRecordings) { index, item ->
                    Column {
                        RecordingElm(
                            Pair(
                                item,
                                groupRecordingsData.find { it.recordingUri == item.uri.toString() }!!
                            ),
                            group = null,
                            onOpenRecordingInfo = onOpenRecordingInfo,
                            onClick = { onPlayRecording(item) },
                            onClickTag = onClickTag,
                            onClickGroupTag = { },
                            isSelectable = editing,
                            isSelected = editing && selectedRecordings.contains(item),
                            onSelect = { onSelect(item) },
                            isPlaying = currentlyPlayingRec?.uri == item.uri,
                            accentColor = color.value
                        )

                        if (index != groupRecordings.size - 1)
                            Divider(Modifier.padding(18.dp, 0.dp, 0.dp, 0.dp))
                    }
                }
            }

            Column (
                Modifier
                    .fillMaxWidth().align(Alignment.BottomStart)
            ) {
                AnimatedVisibility(visible = editing, enter = slideInVertically({it}), exit = slideOutVertically({it})) {
                    Column {
                        ColorChooserRow(
                            chosenColor = color.value,
                            setColor = { colorVal = it.toArgb() }) {
                            onChooseImage(chooseImageCallback)
                        }
                        Spacer (Modifier.height(18.dp))
                    }
                }
                Divider(Modifier.fillMaxWidth())
                Surface (Modifier.fillMaxWidth(), color = MaterialTheme.colors.background) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp), horizontalArrangement = Arrangement.End
                    )
                    {
                        TextButton(
                            onClick = onDeleteGroup,
                            colors = ButtonDefaults.buttonColors(
                                contentColor = MaterialTheme.colors.onBackground,
                                disabledContentColor = MaterialTheme.colors.onBackground.copy(
                                    alpha = .5f
                                ),
                                backgroundColor = Color.Transparent,
                                disabledBackgroundColor = Color.Transparent
                            ),
                            enabled = !editing
                        ) {
                            Text("Delete")
                        }
                        TextButton(
                            onClick = onAddRecordings,
                            colors = ButtonDefaults.buttonColors(
                                contentColor = MaterialTheme.colors.onBackground,
                                disabledContentColor = MaterialTheme.colors.onBackground.copy(
                                    alpha = .5f
                                ),
                                backgroundColor = Color.Transparent,
                                disabledBackgroundColor = Color.Transparent,
                            ),
                            enabled = !editing
                        ) {
                            Text("Add")
                        }
                        TextButton(
                            onClick = {
                                if (editing) {
                                    onSaveEdit(
                                        group,
                                        if (text.isNotBlank()) text else group.name,
                                        imgUri,
                                        color.value.toArgb()
                                    )
                                    groupRecordings.filter { !selectedRecordings.contains(it) }
                                        .forEach { onRemoveRecording(group, it) }
                                }
                                editing = !editing
                            },
                            colors = ButtonDefaults.buttonColors(
                                contentColor = MaterialTheme.colors.onBackground,
                                disabledContentColor = MaterialTheme.colors.onBackground.copy(
                                    alpha = .5f
                                ),
                                backgroundColor = Color.Transparent,
                                disabledBackgroundColor = Color.Transparent
                            )
                        ) {
                            Crossfade(targetState = editing)
                            {
                                if (!it)
                                    Text("Edit")
                                else
                                    Text("Save")
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        OutlinedButton(
                            onClick = {
                                if (groupRecordings.isNotEmpty())
                                    onPlayRecording(groupRecordings[0])
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = color.value,
                                contentColor = if (color.value.luminance() > .65f) Color.Black else Color.White
                            ),
                            enabled = !editing,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(0.dp, Color.Unspecified)
                        ) {
                            Text("Play")
                        }
                    }
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
                groupColors.random().toArgb(),
                UUID.randomUUID().toString()
            ),
                groupRecordings = listOf(
                    Recording(
                        Uri.EMPTY, "Placeholder 1", 150000, 0, "0B",
                        date = LocalDateTime.now().plusDays(1),
                        path = ""
                    ),
                    Recording(Uri.parse("file:///tmp/android.txt"), "Placeholder 2", 0, 0, "0B",
                        path = ""),
                    Recording(Uri.parse("file:///tmp/android2.txt"), "Placeholder 3", 0, 0, "0B",
                        path = ""),
                ),
                groupRecordingsData = listOf(
                    RecordingData(Uri.EMPTY.toString(),
                        ),
                    RecordingData(Uri.parse("file:///tmp/android.txt").toString(),
                        ),
                    RecordingData(Uri.parse("file:///tmp/android2.txt").toString(),
                        ),
                ), {}, {},
                currentlyPlayingRec = null,
                onClickTag = {},
                onDeleteGroup = {},
                onAddRecordings = {},
                onChooseImage = {},
                onSaveEdit = {_, _, _, _ ->},
                onRemoveRecording = {_, _ ->}
            )
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun GroupsListSection(
    recordings: List<Recording>,
    recordingsData: List<RecordingData>,
    groups: List<RecordingGroup>,
    onClickGroup: (RecordingGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(18.dp, 0.dp)
        ) {
            items(groups) {
                Column {
                    val recData = recordingsData.filter { rec -> rec.groupUUID == it.uuid }
                    val recs = recordings.filter { rec -> recData.find { it.recordingUri == rec.uri.toString() } != null }
                    GroupCard(
                        group = it,
                        onClick = { onClickGroup(it) },
                        groupRecordings = recs
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }
}

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Preview
@Composable
fun GroupElmPreview() {

    Surface(Modifier.fillMaxSize()) {
        MicCheckTheme {
            val list = listOf(
                RecordingGroup(
                    "New Group A", uuid = UUID.randomUUID().toString(), fallbackColor = groupColors.random().toArgb()
                ),
                RecordingGroup(
                    "New Group B", uuid = UUID.randomUUID().toString(), fallbackColor = groupColors.random().toArgb()
                ),
                RecordingGroup(
                    "New Group C", uuid = UUID.randomUUID().toString(), fallbackColor = groupColors.random().toArgb()
                ),
            )
            GroupsListSection(
                recordingsData = listOf(),
                groups = list, onClickGroup = {},recordings = listOf())
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun NewGroupScreen(
    onCreate: (String, Uri?, Color) -> Unit,
    onChooseImage: ((Uri) -> Unit) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val (text, setText) = remember { mutableStateOf("") }
    var imgUri: Uri? by remember { mutableStateOf(null) }
    val chooseImageCallback: (Uri) -> Unit = {
        imgUri = it
    }
    var chosenColor by remember { mutableStateOf(groupColors[0]) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Box (Modifier.fillMaxSize()) {
            Column (Modifier.align(Alignment.TopStart)) {
                Spacer(Modifier.height(18.dp))
                Text(
                    "Create Group",
                    style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = 18.dp)
                )
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
                Column (Modifier.padding(18.dp, 0.dp)){
                    GroupCard(
                        group = RecordingGroup(
                            text,
                            imgUri.toString(),
                            chosenColor.toArgb(),
                            ""
                        ),
                        onClick = {},
                        groupRecordings = listOf()
                    )
                }
                Spacer(Modifier.height(18.dp))
                ColorChooserRow(chosenColor = chosenColor, setColor = { chosenColor = it }) {
                    onChooseImage(chooseImageCallback)
                }
            }
            LargeButton(onClick = {
                onCreate(text, imgUri, chosenColor) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-20).dp)
            ) {
                Icon(Icons.Rounded.Done, "Done")
            }
        }

    }
}

@ExperimentalAnimationApi
@Composable
fun ColorChooserRow(chosenColor: Color, setColor: (Color) -> Unit, onChooseImage: () -> Unit) {
    LazyRow (verticalAlignment = Alignment.CenterVertically) {
        item {
            Row {
                Spacer(Modifier.width(18.dp))
                Surface(
                    color = MaterialTheme.colors.surface,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(62.dp)
                        .clickable { onChooseImage() }
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Rounded.InsertPhoto,
                            "Add Picture",
                            modifier = Modifier.align(Alignment.Center),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = .65f)
                        )
                    }
                }
            }
        }
        items (groupColors) {
            Spacer(Modifier.width(12.dp))
            Surface (
                color = it,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .clickable {
                        setColor(it)
                    }
            ) {
                AnimatedVisibility(visible =
                (chosenColor.toArgb() == it.toArgb())) {
                    Box (Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Rounded.Check,
                            null,
                            modifier = Modifier.align(Alignment.Center),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = .65f)
                        )
                    }
                }
            }
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Preview
@Composable
fun NewGroupPreview () {
    MicCheckTheme {
        NewGroupScreen(onCreate = {_, _, _ ->}, onChooseImage = {})
    }
}

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun SelectGroupScreen(
    groups: List<RecordingGroup>,
    recordings: List<Recording>,
    recordingsData: List<RecordingData>,
    onSelect: (RecordingGroup) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(18.dp))
            Text(
                "Select Group",
                style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(start = 18.dp)
            )
            Text(
                "Select a group to add your recording(s) to.",
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(18.dp, 0.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            GroupsListSection(
                recordingsData = recordingsData,
                groups = groups,
                onClickGroup = onSelect,
                recordings = recordings
            )
        }
    }
}

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun SelectRecordingsScreen (
    recordings: List<Recording>,
    recordingsData: List<RecordingData>,
    groups: List<RecordingGroup>,
    onDone: (List<Recording>) -> Unit
) {
    val (text, setText) = remember {
        mutableStateOf("")
    }
    val focusManager = LocalFocusManager.current
    var results by remember {
        mutableStateOf(
            listOf<Recording>()
        )
    }

    val selectedRecordings = remember { mutableStateListOf<Recording>() }
    val onSelect: (Recording) -> Unit = {
        if (selectedRecordings.contains(it))
            selectedRecordings.remove(it)
        else
            selectedRecordings.add(it)
    }

    Surface (color = MaterialTheme.colors.background, modifier = Modifier.fillMaxSize()) {
        Box (Modifier.fillMaxSize()){
            LaunchedEffect(key1 = text) {
                //fuzzywuzzy search
                results =
                    if (text.isBlank())
                        recordings
                    else
                        searchAlgorithm(
                            text,
                            recordings = recordings,
                            recordingsData = listOf(),
                            groups = listOf(),
                            tagFilter = null
                        ) as List<Recording>
            }

            Column {
                Spacer (Modifier.height(18.dp))
                Row {
                    Spacer(Modifier.width(18.dp))
                    Text(
                        if (selectedRecordings.isEmpty()) "Select Recordings"
                        else "${selectedRecordings.size} Recording" +
                                if (selectedRecordings.size != 1) "s" else "",
                        style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 12.dp)
                    )
                }
                Row {
                    TextField(
                        value = text,
                        onValueChange = setText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp, 0.dp, 18.dp, 0.dp),
                        placeholder = {
                            Text(
                                "Search for a recording",
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
                }
                LazyColumn {
                    itemsIndexed(results, key = { _, rec -> rec.uri }) { index, item ->
                        val recData = recordingsData.find { it.recordingUri == item.uri.toString() }!!
                        Column {
                            RecordingElm(
                                Pair(item, recData),
                                groups.find { it.uuid == recData.groupUUID},
                                onOpenRecordingInfo = {},
                                onClick = {
                                    onSelect(item)
                                },
                                onClickTag = {},
                                isSelectable = true,
                                isSelected = selectedRecordings.contains(item),
                                isPlaying = false,
                                onSelect = onSelect,
                                onClickGroupTag = {}
                            )
                            if (index != recordings.size - 1)
                                Divider(Modifier.padding(18.dp, 0.dp, 0.dp, 0.dp))
                        }
                    }
                }
            }

            LargeButton(
                onClick = {

                    onDone(selectedRecordings.toList())
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-20).dp)
            ) {
                Icon(Icons.Rounded.Done, "Done")
            }
        }
    }
}