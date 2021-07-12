package com.example.miccheck

import androidx.compose.animation.*
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miccheck.ui.theme.MicCheckTheme

@Composable
fun Chip (
    text: String,
    color: Color = MaterialTheme.colors.surface,
    contentColor: Color = MaterialTheme.colors.onSurface,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton (
        onClick = onClick,
        modifier =
            modifier
                .height(32.dp),
        elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.outlinedButtonColors(color, contentColor),
        shape = RoundedCornerShape(50),
        border = ButtonDefaults.outlinedBorder.copy(width = 1.dp)
    ) {
        Text (text)
    }
}

@ExperimentalAnimationApi
@Composable
fun BigButton(
    onClick: () -> Unit,
    selected: Boolean,
    text: String,
    icon: ImageVector,
    enabledColor: Color = Color.Black,
    disabledColor: Color = Color.LightGray,
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
            .animateContentSize()
    ) {
        Row (
            modifier =
            Modifier
                .padding(4.dp)
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(),
                exit = fadeOut(),
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
fun BigButtonRow(
    buttons: @Composable () -> Unit
) {
    Row (modifier = Modifier.horizontalScroll(rememberScrollState())) {
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
                "Chip",
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
        BigButtonRow {
            BigButton(
                onClick = { onClick(0) },
                selected = sel == 0,
                text = "Recordings",
                icon = Icons.Default.MicExternalOn
            )
            BigButton(
                onClick = { onClick(1) },
                selected = sel == 1,
                text = "Groups",
                icon = Icons.Default.Inventory2
            )
        }
    }
}