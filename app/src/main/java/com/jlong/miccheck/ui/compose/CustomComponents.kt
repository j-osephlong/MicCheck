package com.jlong.miccheck.ui.compose

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.jlong.miccheck.ui.theme.MicCheckTheme

@Composable
fun StatusBarColor() {
    val systemUiController = rememberSystemUiController()
    val statusBarColor = MaterialTheme.colors.surface
    val darkIcons = !isSystemInDarkTheme()
    SideEffect {
        // Update all of the system bar colors to be transparent, and use
        // dark icons if we're in light theme
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = darkIcons
        )

    }
}

/**
 * Source : https://dev.to/pawegio/handling-back-presses-in-jetpack-compose-50d5
 * Back press override
 */
@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    // Safely update the current `onBack` lambda when a new one is provided
    val currentOnBack by rememberUpdatedState(onBack)
    // Remember in Composition a back callback that calls the `onBack` lambda
    val backCallback = remember {
        object : OnBackPressedCallback(enabled) {
            override fun handleOnBackPressed() {
                currentOnBack()
            }
        }
    }
    // On every successful composition, update the callback with the `enabled` value
    SideEffect {
        backCallback.isEnabled = enabled
    }
    val backDispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current) {
        "No OnBackPressedDispatcherOwner was provided via LocalOnBackPressedDispatcherOwner"
    }.onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, backDispatcher) {
        // Add callback to the backDispatcher
        backDispatcher.addCallback(lifecycleOwner, backCallback)
        // When the effect leaves the Composition, remove the callback
        onDispose {
            backCallback.remove()
        }
    }
}

@Composable
fun TinyChip (
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    val textColor = if (color.luminance() > .5f) Color.Black else Color.White
    Surface (
        shape = RoundedCornerShape(12.dp),
        color = color,
        modifier = Modifier.clickable {onClick() }
    ) {
        Text (
            text.uppercase(),
            style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(8.dp, 4.dp),
            color = textColor.copy(alpha = .7f)
        )
    }
}

@Preview
@Composable
fun TinyChipPreview() {
    MicCheckTheme {
        Surface {
            Surface (Modifier.padding(18.dp)){
                TinyChip(text = "piano", color = MaterialTheme.colors.secondary) {

                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    extraText: String?,
    actionName: String,
    visible: Boolean,
    onClose: () -> Unit,
    action: () -> Unit
) {
    if (visible)
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text(title) },
            text = {
                extraText?.also {
                    Text(it)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = action,
                    colors = ButtonDefaults.buttonColors(
                        contentColor = MaterialTheme.colors.onBackground,
                        backgroundColor = Color.Transparent
                    )
                ) {
                    Text(actionName)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        contentColor = MaterialTheme.colors.onBackground,
                        backgroundColor = Color.Transparent
                    )
                ) {
                    Text("Cancel")
                }
            },
            backgroundColor = MaterialTheme.colors.background,
            shape = RoundedCornerShape(18.dp)
        )
}

@Composable
fun Chip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onIconClick: () -> Unit = {},
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
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.onPrimary.copy(alpha = .6f)
        ),
        shape = RoundedCornerShape(40),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
        modifier = modifier
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
        NewButtons(0) {}
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
                "chip",
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