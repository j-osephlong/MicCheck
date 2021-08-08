package com.jlong.miccheck

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MicExternalOn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.jlong.miccheck.ui.theme.MicCheckTheme

@Composable
fun SeekBar(
    progress: Float = 1f,
    onUpdateProgress: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
//    val dragAmount

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        val width = maxWidth
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        onUpdateProgress(
                            ((it.x.toDp()) / (width)).let {
                                Log.i("SeekProg", "$it")
                                it
                            }
                        )
                    }
                }
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .align(Alignment.TopStart), verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colors.secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .padding(10.dp, 0.dp),
                    shape = RoundedCornerShape(100)
                ) {

                }
            }
            Row(
                Modifier
                    .fillMaxSize()
                    .align(Alignment.TopStart), verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier
                        .padding(10.dp, 0.dp)
                        .fillMaxWidth(progress)
                        .height(6.dp),
                    shape = RoundedCornerShape(100)
                ) {

                }
            }
            BoxWithConstraints(
                Modifier
                    .padding(10.dp, 0.dp)
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {

                            },
                            onDragEnd = {

                            },
                            onHorizontalDrag = { change, dragAmount ->

                            }
                        )
                    }
            ) {
                Row(
                    Modifier
                        .offset(x = max(0.dp, (maxWidth * progress) - 10.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colors.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(20.dp),
                        elevation = 4.dp
                    ) {

                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun SeekbarPreview() {
    Surface(
        color = Color.White,
        modifier = Modifier
            .padding(0.dp)
            .fillMaxWidth()
    ) {
        MicCheckTheme {
            SeekBar(.05f, {})
        }
    }
}

@Composable
fun Chip(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    onIconClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.background,
    contentColor: Color = MaterialTheme.colors.onBackground
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(32.dp)
            .widthIn(min = 84.dp, max = 164.dp),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.outlinedButtonColors(color, contentColor),
        shape = RoundedCornerShape(100),
        border = ButtonDefaults.outlinedBorder.copy(width = 1.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    icon, "Chip Icon",
                    Modifier
                        .clickable(onClick = onIconClick)
                        .size(18.dp)
                )
                if (text.isNotBlank())
                    Spacer(modifier = Modifier.width(8.dp))
            } else {
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text, overflow = TextOverflow.Ellipsis, maxLines = 1,
                modifier = Modifier.widthIn(max = 140.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

    }
}

@Composable
fun LargeButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.onPrimary.copy(alpha = .6f)
        ),
        shape = RoundedCornerShape(40),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp)
    ) {
        Box(Modifier.padding(24.dp, 12.dp)) {
            content()
        }
    }
}

@Composable
fun CircleButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.onPrimary.copy(alpha = .6f)
        ),
        shape = CircleShape,
        elevation = ButtonDefaults.elevation(
            0.dp, 0.dp, 0.dp
        ),
        modifier = Modifier.size(50.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        content()
    }
}

@ExperimentalAnimationApi
@Composable
fun ScreenSelectButton(
    onClick: () -> Unit,
    selected: Boolean,
    text: String,
    icon: ImageVector,
    enabledColor: Color = Color.Black,
    disabledColor: Color = Color.White,
    enabledTextColor: Color = Color.White,
    disabledTextColor: Color = Color.Black,
) {
    val textStyle = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold
    )

    val bgColor by animateColorAsState(targetValue =
        (if (selected) enabledColor else disabledColor)
    )

    val textColor by animateColorAsState(targetValue =
        (if (selected) enabledTextColor else disabledTextColor)
    )

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = bgColor,
            contentColor = textColor
        ),
        shape = RoundedCornerShape(40),
        modifier =
        Modifier
            .padding(0.dp, 0.dp, 12.dp, 0.dp)
//            .animateContentSize()
    ) {
        Row (
            modifier =
            Modifier
                .padding(4.dp)
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(),
                exit = shrinkHorizontally(),
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Row {
                    Icon(
                        icon,
                        "TBD",
                        Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }
            Text(text, style = textStyle)
        }
    }
}

@Composable
fun NewButtons(
    buttonPos: Int,
    onClick: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp, 0.dp, 12.dp, 18.dp),
        color = Color.Transparent
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Surface(
                Modifier.fillMaxWidth(.75f),
                shape = RoundedCornerShape(100),
                color = MaterialTheme.colors.secondary
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val width = maxWidth
                    val offset by animateDpAsState(
                        when (buttonPos) {
                            0 -> 0.dp
                            else -> width / 2
                        },
                        spring(dampingRatio = Spring.DampingRatioLowBouncy)
                    )
                    Surface(
                        Modifier
                            .fillMaxWidth(.5f)
                            .align(Alignment.TopStart)
                            .clickable {
                                onClick(0)
                            }
                            .clip(RoundedCornerShape(100)),
                        shape = RoundedCornerShape(100),
                        color = Color.Transparent,
                    ) {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Record",
                                style = MaterialTheme.typography.h6.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colors.onPrimary.copy(
                                        alpha = .7f
                                    )
                                ),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    Surface(
                        Modifier
                            .fillMaxWidth(.5f)
                            .align(Alignment.TopEnd)
                            .clickable {
                                onClick(1)
                            }
                            .clip(RoundedCornerShape(100)),
                        shape = RoundedCornerShape(100),
                        color = Color.Transparent
                    ) {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Play",
                                style = MaterialTheme.typography.h6.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colors.onPrimary.copy(
                                        alpha = .7f
                                    )
                                ),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    Surface(
                        Modifier
                            .fillMaxWidth(.5f)
                            .align(Alignment.TopStart)
                            .offset(x = offset),
                        shape = RoundedCornerShape(100),
                        color = MaterialTheme.colors.primary,
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Crossfade(
                                targetState = buttonPos,
                                modifier = Modifier
                                    .fillMaxWidth(),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (it == 0)
                                        Text(
                                            "Record",
                                            style = MaterialTheme.typography.h6.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colors.onPrimary.copy(
                                                    alpha = .7f
                                                )
                                            ),
                                            modifier = Modifier
                                                .padding(12.dp)
                                        )
                                    else
                                        Text(
                                            "Play",
                                            style = MaterialTheme.typography.h6.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colors.onPrimary.copy(
                                                    alpha = .7f
                                                )
                                            ),
                                            modifier = Modifier
                                                .padding(12.dp)
                                        )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun NewButtonPreview() {
    MicCheckTheme {
        NewButtons(0, {})
    }
}

@Composable
fun ScreenSelectRow(
    modifier: Modifier = Modifier,
    buttons: @Composable () -> Unit
) {
    Row(modifier = modifier.horizontalScroll(rememberScrollState())) {
        Spacer(Modifier.width(18.dp))
        buttons()
    }
}

@Preview
@Composable
fun ChipPreview () {
    MicCheckTheme {
        Surface {
            Chip(
                "deez nuts haha wow wowow",
                onClick = {}
            )
        }
    }
}

@ExperimentalAnimationApi
@Preview
@Composable
fun ButtonPreview () {
    var sel by remember {
        mutableStateOf(0)
    }

    val onClick: (Int)->Unit = {
        sel = it
    }
    Surface (Modifier.fillMaxWidth()) {
        ScreenSelectRow {
            ScreenSelectButton(
                onClick = { onClick(0) },
                selected = sel == 0,
                text = "Recordings",
                icon = Icons.Rounded.MicExternalOn
            )
            ScreenSelectButton(
                onClick = { onClick(1) },
                selected = sel == 1,
                text = "Groups",
                icon = Icons.Rounded.Inventory2
            )
        }
    }
}