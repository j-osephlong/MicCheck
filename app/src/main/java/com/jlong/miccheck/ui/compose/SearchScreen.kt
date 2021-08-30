package com.jlong.miccheck.ui.compose

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jlong.miccheck.*
import me.xdrop.fuzzywuzzy.FuzzySearch

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun SearchScreen(
    recordings: List<Recording>,
    recordingsData: List<RecordingData>,
    groups: List<RecordingGroup>,
    tagFilter: Tag?,
    onOpenRecordingInfo: (Recording) -> Unit,
    onStartPlayback: (Recording) -> Unit,
    onOpenPlayback: () -> Unit,
    onOpenSelectTag: () -> Unit,
    onRemoveTagFilter: () -> Unit,
    onOpenTimeStamp: (Recording, TimeStamp) -> Unit,
    onOpenGroup: (RecordingGroup) -> Unit
) {
    val (text, setText) = remember {
        mutableStateOf("")
    }
    val focusManager = LocalFocusManager.current
    var results by remember {
        mutableStateOf(
            listOf<Searchable>()
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(0.dp, 18.dp, 0.dp, 0.dp)
        ) {
            Row {
                Spacer(Modifier.width(18.dp))
                Text(
                    "Search",
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
            Spacer(Modifier.height(12.dp))
            Row {
                Spacer(Modifier.width(18.dp))
                Chip(
                    tagFilter?.name ?: "Filter by tag",
                    if (tagFilter == null) onOpenSelectTag else onRemoveTagFilter,
                    icon = if (tagFilter == null) Icons.Rounded.Add else Icons.Rounded.Close,
                    onIconClick = {
                        if (tagFilter != null) {
                            onRemoveTagFilter()
                            results = listOf()
                        }
                    }
                )
            }

            LaunchedEffect(key1 = text, key2 = tagFilter) {
                //fuzzywuzzy search
                results =
                    if (text.isBlank() && tagFilter != null)
                        recordings
                            .filter { rec ->
                                recordingsData.find { it.recordingUri == rec.uri.toString() }!!
                                    .tags.find { it.name == tagFilter.name } != null
                            }
                    else
                        searchAlgorithm(
                            text,
                            recordings = recordings,
                            recordingsData = recordingsData,
                            groups = groups,
                            tagFilter = tagFilter
                        )
            }

            ResultsList(
                results = results,
                recordingsData = recordingsData,
                groups = groups,
                onOpenRecordingInfo = onOpenRecordingInfo,
                onStartPlayback = onStartPlayback,
                onOpenPlayback = onOpenPlayback,
                onOpenTimeStamp = { uri, timeStamp ->
                    onOpenTimeStamp(recordings.find { it.uri == uri }!!, timeStamp)
                },
                onOpenGroup = onOpenGroup
            )
        }
    }
}

fun searchAlgorithm(
    query: String,
    recordings: List<Recording>,
    recordingsData: List<RecordingData>,
    groups: List<RecordingGroup>,
    tagFilter: Tag?
): List<Searchable> {

    val filteredList = recordings.toMutableList().filter { rec ->
        if (tagFilter == null)
            true
        else {
            recordingsData.find { it.recordingUri == rec.uri.toString() }!!.tags.find { it.name == tagFilter.name } != null
        }
    }

    val timestampsList = filteredList.let { list ->
        val newList: MutableList<TimeStamp> = mutableListOf()
        if (recordingsData.isNotEmpty())
            list.forEach { rec ->
                newList +=
                    recordingsData.find { it.recordingUri == rec.uri.toString() }!!.timeStamps
            }
        newList
    }

    val groupsList = if (tagFilter == null) groups else listOf()

    val combinedList: MutableList<Searchable> = (filteredList + timestampsList + groupsList).toMutableList()

    val transformedResults = FuzzySearch.extractAll(
        query, combinedList, { item -> item.name }, 35
    )
        .also { list -> list.sortByDescending { it.score } }         //sort results by score desc
        .map { it.referent!! }

    return transformedResults
}

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun ResultsList(
    results: List<Searchable>,
    groups: List<RecordingGroup>,
    recordingsData: List<RecordingData>,
    onOpenRecordingInfo: (Recording) -> Unit,
    onStartPlayback: (Recording) -> Unit,
    onOpenPlayback: () -> Unit,
    onOpenTimeStamp: (Uri, TimeStamp) -> Unit,
    onOpenGroup: (RecordingGroup) -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumn(state = listState) {
        itemsIndexed(results) { index, item ->
            Column {
                if (item is Recording) {
                    val pair = Pair(
                        item,
                        recordingsData.find { it.recordingUri == item.uri.toString() }!!
                    )
                    RecordingElm(
                        pair,
                        group = groups.find { it.uuid == pair.second.groupUUID },
                        onOpenRecordingInfo = onOpenRecordingInfo,
                        onClick = {
                            onStartPlayback(item)
                            onOpenPlayback()
                        },
                        onClickTag = {},
                        onClickGroupTag = onOpenGroup,
                        isSelectable = false,
                        isSelected = false,
                        onSelect = { },
                        isPlaying = false
                    )
                }
                if (item is TimeStamp) {
                    TimestampSearchElm(
                        timeStamp = item,
                        { onOpenTimeStamp(Uri.parse(item.recordingUri), item) })
                }
                if (item is RecordingGroup)
                    GroupSearchElm(group = item) {
                        onOpenGroup(item)
                    }
                if (index != results.size - 1)
                    Divider(Modifier.padding(18.dp, 0.dp, 0.dp, 0.dp))
            }
        }
    }

    LaunchedEffect(key1 = results) {
        listState.animateScrollToItem(0)
    }
}

@ExperimentalAnimationApi
@Composable
private fun TimestampSearchElm(
    timeStamp: TimeStamp,
    onClick: () -> Unit
) {
    var showDescription by remember { mutableStateOf(false) }
    val descriptionButtonDegrees by animateFloatAsState(
        targetValue = if (showDescription) 180f else 0f
    )
    Column(
        Modifier
            .animateContentSize()
            .clickable { onClick() }
            .padding(18.dp, 0.dp)) {
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${timeStamp.recordingName},",
                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Timestamp at ${timeStamp.timeMilli.toTimestamp()}",
                    style = MaterialTheme.typography.h6
                )
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
            }
        }
        if (timeStamp.description != null) {
            Spacer(Modifier.height(4.dp))
            AnimatedVisibility(visible = showDescription) {
                Text(timeStamp.description!!)
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
fun GroupSearchElm (
    group: RecordingGroup,
    onClick: () -> Unit
) {
    Row (
        Modifier
            .padding(18.dp)
            .clickable { onClick() }
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                group.name,
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("Group of recordings")
        }
        Surface(
            Modifier.size(42.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(group.fallbackColor)
        ) {

        }
    }
}
