package com.example.miccheck

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MicExternalOn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miccheck.ui.theme.MicCheckTheme

@Composable
fun Chip(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    onIconClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.surface.copy(alpha = .2f),
    contentColor: Color = MaterialTheme.colors.onSurface
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(32.dp)
            .widthIn(min = 84.dp, max = 142.dp),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.outlinedButtonColors(color, contentColor),
        shape = RoundedCornerShape(100),
        border = ButtonDefaults.outlinedBorder.copy(width = 1.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(icon, "Chip Icon",
                    Modifier
                        .clickable(onClick = onIconClick)
                        .size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(text, overflow = TextOverflow.Ellipsis, maxLines = 1)
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
            backgroundColor = MaterialTheme.colors.primaryVariant,
            contentColor = MaterialTheme.colors.onPrimary.copy(alpha = .6f)
        ),
        shape = RoundedCornerShape(40)
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
            backgroundColor = MaterialTheme.colors.primaryVariant.copy(
                alpha = .25f
            ),
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
fun ScreenSelectRow(
    modifier: Modifier = Modifier,
    buttons: @Composable () -> Unit
) {
    Row(modifier = modifier.horizontalScroll(rememberScrollState())) {
        Spacer(Modifier.width(12.dp))
        buttons()
    }
}

@Preview
@Composable
fun ChipPreview () {
    MicCheckTheme {
        Surface {
            Chip(
                "ChipChipChipChip",
                onClick = {},
                icon = Icons.Default.Cancel
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
                icon = Icons.Default.MicExternalOn
            )
            ScreenSelectButton(
                onClick = { onClick(1) },
                selected = sel == 1,
                text = "Groups",
                icon = Icons.Default.Inventory2
            )
        }
    }
}