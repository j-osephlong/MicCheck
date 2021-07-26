package com.example.miccheck

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.MicExternalOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.miccheck.ui.theme.MicCheckTheme
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*


@ExperimentalMaterialApi
@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun RecordingsScreen(
    recordings: List<Recording>,
    recordingsData: List<RecordingData>,
    currentPlaybackRec: Recording?,
    onStartPlayback: (Recording) -> Unit,
    onOpenPlayback: () -> Unit,
    onOpenRecordingInfo: (Recording) -> Unit
) {
    var selectedScreen by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(18.dp))
        ScreenSelectRow {
            ScreenSelectButton(
                onClick = { selectedScreen = 0; Log.e("WHICHONE", "RECORDINGS " + selectedScreen) },
                selected = selectedScreen == 0,
                text = "Recordings",
                icon = Icons.Default.MicExternalOn
            )
            ScreenSelectButton(
                onClick = { selectedScreen = 1; Log.e("WHICHONE", "GROUPS " + selectedScreen) },
                selected = selectedScreen == 1,
                text = "Groups",
                icon = Icons.Default.Inventory2
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        when (selectedScreen) {
            else ->
                RecordingsList(
                    recordings = recordings,
                    recordingsData = recordingsData,
                    currentPlaybackRec = currentPlaybackRec,
                    onStartPlayback = onStartPlayback,
                    onOpenPlayback = onOpenPlayback,
                    onOpenRecordingInfo = onOpenRecordingInfo
                )
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun RecordingsList(
    recordings: List<Recording>,
    recordingsData: List<RecordingData>,
    currentPlaybackRec: Recording?,
    onStartPlayback: (Recording) -> Unit,
    onOpenPlayback: () -> Unit,
    onOpenRecordingInfo: (Recording) -> Unit
) {

    Log.e("RL", "Creating grouped")
    val rec = recordings.toMutableList().apply { sortByDescending { it.uri.toString() } }
    val recData =
        recordingsData.toMutableList().apply { sortByDescending { it.recordingUri } }
    val recordingsGrouped = rec.zip(recData).groupBy { it.first.toDateKey() }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            recordingsGrouped.forEach { (_, recordings) ->
                stickyHeader {
                    DateHeader(recordings[0].first.date)
                }

                itemsIndexed(recordings, key = { _, rec -> rec.first.uri }) { index, item ->
                    if (index == 0)
                        Spacer(modifier = Modifier.height(0.dp))
                    Column {
                        RecordingElm(
                            item,
                            onOpenRecordingInfo = onOpenRecordingInfo,
                            onClick = {
                                if (item.first == currentPlaybackRec)
                                    onOpenPlayback()
                                else {
                                    onStartPlayback(item.first)
                                    onOpenPlayback()
                                }
                            }
                        )
                        if (index != recordings.size - 1)
                            Divider(Modifier.padding(18.dp, 0.dp, 0.dp, 0.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(date: LocalDateTime) {
    Surface(
        color = MaterialTheme.colors.background,
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp, 0.dp, 0.dp, 8.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
        ) {
            Text(
                date.format(
                    DateTimeFormatter.ofPattern("LLLL d")
                        .withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault())
                ) +
                        when (date.dayOfMonth % 10) {
                            1 -> if (date.dayOfMonth == 11) "th" else "st"
                            2 -> if (date.dayOfMonth == 12) "th" else "nd"
                            3 -> if (date.dayOfMonth == 13) "th" else "rd"
                            else -> "th"
                        } +
                        date.format(
                            DateTimeFormatter.ofPattern(", yyyy")
                                .withLocale(Locale.getDefault())
                                .withZone(ZoneId.systemDefault())
                        ),
                style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.ExtraBold)
            )
            Spacer(Modifier.height(8.dp))
            Divider()
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun GroupsList(
    recordings: List<Recording>,
    recordingsData: List<RecordingData>,
    groups: List<RecordingGroup>
) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(groups) { group ->
                val groupRecordings = recordingsData.filter { recData ->
                    recData.group?.let {
                        it.uuid == group.uuid
                    } == true
                }.let { list ->
                    list.sortedByDescending { it.recordingUri }.zip(
                        recordings.sortedByDescending { it.uri.toString() }.filter { rec ->
                            list.find { rec.uri.toString() == it.recordingUri } != null
                        }
                    )
                }

                GroupElm(group = group,
                    numRecordings = groupRecordings.size,
                    lengthMilli = groupRecordings.sumOf {
                        it.second.duration.toLong()
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun GroupElm(
    group: RecordingGroup,
    numRecordings: Int,
    lengthMilli: Long
) {
    Surface(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {

            Image(
                bitmap = ImageBitmap.imageResource(id = R.drawable.photo).asAndroidBitmap()
                    .fastblur(1f, 20)!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopStart),
                contentScale = ContentScale.Crop
            )
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopStart),
                color = MaterialTheme.colors.background.copy(alpha = .2f)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(
                            group.name,
                            style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 2
                        )
                        Text(
                            "$numRecordings Recordings • " +
                                    (when {
                                        lengthMilli >= 86400000 -> "${lengthMilli / (1000 * 60 * 60 * 24)} Day${if (lengthMilli / (1000 * 60 * 60 * 24) > 1) "s" else ""}"
                                        lengthMilli / 60000 >= 60 -> "${lengthMilli / (1000 * 60 * 60) % 24} Hour${if (lengthMilli / (1000 * 60 * 60) % 24 > 1) "s" else ""}"
                                        lengthMilli / 1000 >= 60 -> "${lengthMilli / (1000 * 60) % 60} Minute${if (lengthMilli / (1000 * 60) % 60 > 1) "s" else ""}"
                                        lengthMilli >= 1000 -> "${lengthMilli / 1000 % 60} Minute${if (lengthMilli / 1000 % 60 > 1) "s" else ""}"
                                        else -> "Really short"
                                    }),
                            style = MaterialTheme.typography.h6,
                            maxLines = 1
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(
                                Icons.Default.MoreVert, null
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Stack Blur v1.0 from
 * http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
 * Java Author: Mario Klingemann <mario at quasimondo.com>
 * http://incubator.quasimondo.com
 *
 * created Feburary 29, 2004
 * Android port : Yahel Bouaziz <yahel at kayenko.com>
 * http://www.kayenko.com
 * ported april 5th, 2012
 *
 * This is a compromise between Gaussian Blur and Box blur
 * It creates much better looking blurs than Box Blur, but is
 * 7x faster than my Gaussian Blur implementation.
 *
 * I called it Stack Blur because this describes best how this
 * filter works internally: it creates a kind of moving stack
 * of colors whilst scanning through the image. Thereby it
 * just has to add one new block of color to the right side
 * of the stack and remove the leftmost color. The remaining
 * colors on the topmost layer of the stack are either added on
 * or reduced by one, depending on if they are on the right or
 * on the left side of the stack.
 *
 * If you are using this algorithm in your code please add
 * the following line:
 * Stack Blur Algorithm by Mario Klingemann <mario></mario>@quasimondo.com>
</yahel></mario> */
fun Bitmap.fastblur(scale: Float, radius: Int): Bitmap? {
    var sentBitmap = this
    val width = Math.round(sentBitmap.width * scale)
    val height = Math.round(sentBitmap.height * scale)
    sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false)
    val bitmap = sentBitmap.copy(sentBitmap.config, true)
    if (radius < 1) {
        return null
    }
    val w = bitmap.width
    val h = bitmap.height
    val pix = IntArray(w * h)
    Log.e("pix", w.toString() + " " + h + " " + pix.size)
    bitmap.getPixels(pix, 0, w, 0, 0, w, h)
    val wm = w - 1
    val hm = h - 1
    val wh = w * h
    val div = radius + radius + 1
    val r = IntArray(wh)
    val g = IntArray(wh)
    val b = IntArray(wh)
    var rsum: Int
    var gsum: Int
    var bsum: Int
    var x: Int
    var y: Int
    var i: Int
    var p: Int
    var yp: Int
    var yi: Int
    var yw: Int
    val vmin = IntArray(Math.max(w, h))
    var divsum = div + 1 shr 1
    divsum *= divsum
    val dv = IntArray(256 * divsum)
    i = 0
    while (i < 256 * divsum) {
        dv[i] = i / divsum
        i++
    }
    yi = 0
    yw = yi
    val stack = Array(div) { IntArray(3) }
    var stackpointer: Int
    var stackstart: Int
    var sir: IntArray
    var rbs: Int
    val r1 = radius + 1
    var routsum: Int
    var goutsum: Int
    var boutsum: Int
    var rinsum: Int
    var ginsum: Int
    var binsum: Int
    y = 0
    while (y < h) {
        bsum = 0
        gsum = bsum
        rsum = gsum
        boutsum = rsum
        goutsum = boutsum
        routsum = goutsum
        binsum = routsum
        ginsum = binsum
        rinsum = ginsum
        i = -radius
        while (i <= radius) {
            p = pix[yi + Math.min(wm, Math.max(i, 0))]
            sir = stack[i + radius]
            sir[0] = p and 0xff0000 shr 16
            sir[1] = p and 0x00ff00 shr 8
            sir[2] = p and 0x0000ff
            rbs = r1 - Math.abs(i)
            rsum += sir[0] * rbs
            gsum += sir[1] * rbs
            bsum += sir[2] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
            i++
        }
        stackpointer = radius
        x = 0
        while (x < w) {
            r[yi] = dv[rsum]
            g[yi] = dv[gsum]
            b[yi] = dv[bsum]
            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum
            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]
            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]
            if (y == 0) {
                vmin[x] = Math.min(x + radius + 1, wm)
            }
            p = pix[yw + vmin[x]]
            sir[0] = p and 0xff0000 shr 16
            sir[1] = p and 0x00ff00 shr 8
            sir[2] = p and 0x0000ff
            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]
            rsum += rinsum
            gsum += ginsum
            bsum += binsum
            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer % div]
            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]
            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]
            yi++
            x++
        }
        yw += w
        y++
    }
    x = 0
    while (x < w) {
        bsum = 0
        gsum = bsum
        rsum = gsum
        boutsum = rsum
        goutsum = boutsum
        routsum = goutsum
        binsum = routsum
        ginsum = binsum
        rinsum = ginsum
        yp = -radius * w
        i = -radius
        while (i <= radius) {
            yi = Math.max(0, yp) + x
            sir = stack[i + radius]
            sir[0] = r[yi]
            sir[1] = g[yi]
            sir[2] = b[yi]
            rbs = r1 - Math.abs(i)
            rsum += r[yi] * rbs
            gsum += g[yi] * rbs
            bsum += b[yi] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
            if (i < hm) {
                yp += w
            }
            i++
        }
        yi = x
        stackpointer = radius
        y = 0
        while (y < h) {

            // Preserve alpha channel: ( 0xff000000 & pix[yi] )
            pix[yi] = -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum
            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]
            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]
            if (x == 0) {
                vmin[y] = Math.min(y + r1, hm) * w
            }
            p = x + vmin[y]
            sir[0] = r[p]
            sir[1] = g[p]
            sir[2] = b[p]
            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]
            rsum += rinsum
            gsum += ginsum
            bsum += binsum
            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer]
            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]
            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]
            yi += w
            y++
        }
        x++
    }
    Log.e("pix", w.toString() + " " + h + " " + pix.size)
    bitmap.setPixels(pix, 0, w, 0, 0, w, h)
    return bitmap
}

@Preview
@Composable
fun GroupElmPreview() {
    Surface(Modifier.fillMaxWidth()) {
        MicCheckTheme {
            GroupElm(
                group = RecordingGroup(
                    "New Group", uuid = UUID.randomUUID().toString()
                ), numRecordings = 0, lengthMilli = 7200000L
            )
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Preview
@Composable
fun RecordingScreenPreview() {
    RecordingsScreen(
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
        ),
        currentPlaybackRec = null,
        onStartPlayback = {},
        onOpenPlayback = { /*TODO*/ },
        onOpenRecordingInfo = {}
    )
}

@ExperimentalMaterialApi
@Composable
fun RecordingElm(
    rec: Pair<Recording, RecordingData>,
    modifier: Modifier = Modifier,
    onOpenRecordingInfo: (Recording) -> Unit,
    onClick: () -> Unit
) {
    Card(
        elevation = 0.dp,
        onClick = onClick,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(18.dp, 18.dp, 0.dp, 18.dp)
        ) {

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row {
                        Text(
                            rec.first.name,
                            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            " - " +
                                    (
                                            if (((rec.first.duration / 1000) / 60) / 60 > 0)
                                                (((rec.first.duration / 1000) / 360).toString() + ":")
                                            else
                                                ""
                                            ) + //hours
                                    ((rec.first.duration / 1000) / 60) % 60 + ":" + //minutes
                                    ((rec.first.duration / 1000) % 60).toString(), //seconds
                            style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Normal)
                        )
                    }

                    Text(
                        "Recorded " + rec.first.date.format(
                            DateTimeFormatter.ofPattern("LLLL d, h:mm a")
                                .withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault())
                        )
                    )
                }

                IconButton(
                    onClick = { onOpenRecordingInfo(rec.first) },
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 18.dp, 0.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Icon(Icons.Default.Launch, "More Option - Recording")
                }
            }

            if (rec.second.tags.isNotEmpty())
                Spacer(Modifier.height(8.dp))
            LazyRow {
                itemsIndexed(rec.second.tags) { index, item ->
                    if (index < 4) {
                        Row {
                            Chip(
                                text = item.name,
                                onClick = { }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }
            }
        }
    }
}
