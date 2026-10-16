package dev.aezochka.m3ekeyboard.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aezochka.m3ekeyboard.R
import dev.aezochka.m3ekeyboard.settings.KeyboardPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun KeyboardView(
    prefs: KeyboardPrefs,
    initialLayout: String,
    enterLabel: String?,
    onText: (String) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    onTap: () -> Unit,
    onLayoutChanged: (String) -> Unit,
) {
    var layout by remember { mutableStateOf(initialLayout) }
    var layer by remember { mutableStateOf(Layer.LETTERS) }
    var shift by remember { mutableStateOf(ShiftState.OFF) }

    val scheme = MaterialTheme.colorScheme
    val keyBg = scheme.surfaceContainerHigh
    val keyBgPressed = scheme.surfaceContainerHighest
    val specialBg = scheme.surfaceContainerHighest
    val specialBgPressed = scheme.surfaceContainerHigh
    val specialContent = scheme.onSurfaceVariant
    val activeBg = scheme.secondaryContainer
    val activeContent = scheme.onSecondaryContainer
    val rowHeight = (56f * prefs.keyHeightScale).dp

    val spaceLabel = if (layout == "ru") R.string.lang_name_ru else R.string.lang_name_en

    fun handle(key: Key) {
        when (key) {
            is Key.Char -> {
                val c = if (layer == Layer.LETTERS && shift != ShiftState.OFF) key.upper else key.base
                onText(c)
                if (shift == ShiftState.ON) shift = ShiftState.OFF
            }
            is Key.Text -> onText(key.text)
            Key.Space -> onText(" ")
            Key.Enter -> onEnter()
            Key.Backspace -> onDelete()
            Key.Shift -> shift = when (shift) {
                ShiftState.OFF -> ShiftState.ON
                ShiftState.ON -> ShiftState.LOCKED
                ShiftState.LOCKED -> ShiftState.OFF
            }
            Key.Symbols -> layer = Layer.SYMBOLS
            Key.Letters -> layer = Layer.LETTERS
            Key.LayoutSwitch -> {
                layout = if (layout == "ru") "en" else "ru"
                onLayoutChanged(layout)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLowest)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Layouts.rows(layout, layer).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { key ->
                    when (key) {
                        is Key.Char -> KeyCap(
                            modifier = Modifier.weight(key.weight).fillMaxHeight(),
                            label = if (layer == Layer.LETTERS && shift != ShiftState.OFF) key.upper else key.base,
                            container = keyBg,
                            pressedContainer = keyBgPressed,
                            contentColor = scheme.onSurface,
                            onTap = { onTap(); handle(key) },
                        )
                        is Key.Text -> KeyCap(
                            modifier = Modifier.weight(key.weight).fillMaxHeight(),
                            label = key.text,
                            fontSize = 17.sp,
                            container = specialBg,
                            pressedContainer = specialBgPressed,
                            contentColor = specialContent,
                            onTap = { onTap(); handle(key) },
                        )
                        Key.Shift -> KeyCap(
                            modifier = Modifier.weight(key.weight).fillMaxHeight(),
                            icon = when (shift) {
                                ShiftState.OFF -> Icons.Outlined.KeyboardArrowUp
                                ShiftState.ON -> Icons.Filled.KeyboardArrowUp
                                ShiftState.LOCKED -> Icons.Filled.KeyboardCapslock
                            },
                            container = if (shift == ShiftState.OFF) specialBg else activeBg,
                            pressedContainer = specialBgPressed,
                            contentColor = if (shift == ShiftState.OFF) specialContent else activeContent,
                            border = shift == ShiftState.LOCKED,
                            borderColor = scheme.secondary,
                            onTap = { onTap(); handle(key) },
                        )
                        Key.Backspace -> KeyCap(
                            modifier = Modifier.weight(key.weight).fillMaxHeight(),
                            icon = Icons.Filled.Backspace,
                            container = specialBg,
                            pressedContainer = specialBgPressed,
                            contentColor = specialContent,
                            onTap = { onTap() },
                            repeatable = true,
                            repeatAction = onDelete,
                        )
                        Key.Space -> KeyCap(
                            modifier = Modifier.weight(key.weight).fillMaxHeight(),
                            label = stringResource(spaceLabel),
                            fontSize = 13.sp,
                            container = specialBg,
                            pressedContainer = specialBgPressed,
                            contentColor = specialContent,
                            shape = RoundedCornerShape(percent = 50),
                            onTap = { onTap(); handle(key) },
                        )
                        Key.Enter -> KeyCap(
                            modifier = Modifier.weight(key.weight).fillMaxHeight(),
                            label = enterLabel ?: "↵",
                            fontSize = if ((enterLabel?.length ?: 1) > 4) 12.sp else 16.sp,
                            container = scheme.tertiaryContainer,
                            pressedContainer = scheme.tertiary,
                            contentColor = scheme.onTertiaryContainer,
                            onTap = { onTap(); handle(key) },
                        )
                        Key.Symbols -> KeyCap(
                            modifier = Modifier.weight(key.weight).fillMaxHeight(),
                            label = "?123",
                            fontSize = 15.sp,
                            container = specialBg,
                            pressedContainer = specialBgPressed,
                            contentColor = specialContent,
                            onTap = { onTap(); handle(key) },
                        )
                        Key.Letters -> KeyCap(
                            modifier = Modifier.weight(key.weight).fillMaxHeight(),
                            label = "ABC",
                            fontSize = 15.sp,
                            container = specialBg,
                            pressedContainer = specialBgPressed,
                            contentColor = specialContent,
                            onTap = { onTap(); handle(key) },
                        )
                        Key.LayoutSwitch -> KeyCap(
                            modifier = Modifier.weight(key.weight).fillMaxHeight(),
                            label = "🌐",
                            fontSize = 18.sp,
                            container = specialBg,
                            pressedContainer = specialBgPressed,
                            contentColor = specialContent,
                            onTap = { onTap(); handle(key) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeyCap(
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector? = null,
    fontSize: TextUnit = 21.sp,
    container: Color,
    pressedContainer: Color,
    contentColor: Color,
    shape: Shape = RoundedCornerShape(18.dp),
    border: Boolean = false,
    borderColor: Color = Color.Unspecified,
    onTap: () -> Unit = {},
    repeatable: Boolean = false,
    repeatAction: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "scale",
    )
    val bg by animateColorAsState(
        targetValue = if (pressed) pressedContainer else container,
        label = "bg",
    )
    if (repeatable && repeatAction != null) {
        LaunchedEffect(pressed) {
            if (pressed) {
                repeatAction()
                delay(320)
                while (isActive) {
                    repeatAction()
                    delay(45)
                }
            }
        }
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .let { m ->
                if (border && borderColor != Color.Unspecified) {
                    m.border(2.dp, borderColor, shape)
                } else m
            }
            .background(bg, shape)
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null) { onTap() },
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
        } else if (label != null) {
            Text(
                text = label,
                color = contentColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}
