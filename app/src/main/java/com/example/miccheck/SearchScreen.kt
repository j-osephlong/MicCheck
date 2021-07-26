package com.example.miccheck

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import me.xdrop.fuzzywuzzy.FuzzySearch

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
    onRemoveTagFilter: () -> Unit
) {
    val (text, setText) = remember {
        mutableStateOf("")
    }
    val focusManager = LocalFocusManager.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(0.dp, 18.dp, 0.dp, 0.dp)
    ) {
        Row {
            Spacer(Modifier.width(18.dp))
            Text(
                "Search",
                style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.ExtraBold),
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
                        "Search for a recording or group",
                        style = MaterialTheme.typography.h6
                    )
                },
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
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )
        }
        Spacer(Modifier.height(12.dp))
        Row {
            Spacer(Modifier.width(18.dp))
            Chip(
                tagFilter?.name ?: "Filter by tag",
                if (tagFilter == null) onOpenSelectTag else {
                    {}
                },
                icon = if (tagFilter == null) Icons.Default.Add else Icons.Default.Remove,
                onIconClick = if (tagFilter == null) {
                    {}
                } else onRemoveTagFilter
            )
        }

        var results by remember {
            mutableStateOf(
                listOf<Pair<Recording, RecordingData>>()
            )
        }

        LaunchedEffect(key1 = text) {
            //fuzzywuzzy search
            if (text.isBlank() && tagFilter != null)
                results = recordings.map {
                    Pair(
                        it,
                        recordingsData.find { recData -> recData.recordingUri == it.uri.toString() }!!
                    )
                }
                    .filter { pair ->
                        pair.second.tags.find { it.name == tagFilter.name } != null
                    }
            else
                results = FuzzySearch.extractAll(
                    text, recordings.toMutableList().filter { rec ->
                        if (tagFilter == null)
                            true
                        else {
                            recordingsData.find { it.recordingUri == rec.uri.toString() }!!.tags.find { it.name == tagFilter.name } != null
                        }
                    }, { x -> x.name }, 35
                ).also { list ->
                    list.sortByDescending { it.score }
                }.let { list ->
                    list.map { it.referent }.let { recList ->
                        recList.map {
                            Pair(
                                it,
                                recordingsData.find { recData -> recData.recordingUri == it.uri.toString() }!!
                            )
                        }
                    }
                }
        }

        ResultsList(
            results = results,
            onOpenRecordingInfo = onOpenRecordingInfo,
            onStartPlayback = onStartPlayback,
            onOpenPlayback = onOpenPlayback
        )
    }
}

@ExperimentalMaterialApi
@Composable
fun ResultsList(
    results: List<Pair<Recording, RecordingData>>,
    onOpenRecordingInfo: (Recording) -> Unit,
    onStartPlayback: (Recording) -> Unit,
    onOpenPlayback: () -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumn(state = listState) {
        itemsIndexed(results, key = { _, rec -> rec.first.uri }) { index, item ->
            Column {
                RecordingElm(
                    item,
                    onOpenRecordingInfo = onOpenRecordingInfo,
                    onClick = {
                        onStartPlayback(item.first)
                        onOpenPlayback()
                    },
                    onClickTag = {}
                )
                if (index != results.size - 1)
                    Divider(Modifier.padding(18.dp, 0.dp, 0.dp, 0.dp))
            }
        }
    }

    LaunchedEffect(key1 = results) {
        listState.animateScrollToItem(0)
    }
}
