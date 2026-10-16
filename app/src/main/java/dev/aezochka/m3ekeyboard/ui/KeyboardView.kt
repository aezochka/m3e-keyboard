package dev.aezochka.m3ekeyboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aezochka.m3ekeyboard.R
import dev.aezochka.m3ekeyboard.settings.KeyboardPrefs
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun KeyboardView(
    prefs: KeyboardPrefs,
    initialLayout: String,
    enterLabel: String?,
    suggestions: List<String> = emptyList(),
    clipboardEntries: List<String> = emptyList(),
    onText: (String) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    onTap: () -> Unit,
    onStrongTap: () -> Unit = onTap,
    onSuggestion: (String) -> Unit = {},
    onClipboardPaste: (String) -> Unit = {},
    onClipboardDelete: (String) -> Unit = {},
    onClipboardClear: () -> Unit = {},
    onOpenApp: () -> Unit = {},
    onLayoutChanged: (String) -> Unit,
) {
    var layout by remember { mutableStateOf(initialLayout) }
    var layer by remember { mutableStateOf(Layer.LETTERS) }
    var shift by remember { mutableStateOf(ShiftState.OFF) }
    var accentChoices by remember { mutableStateOf<List<String>?>(null) }
    var clipboardOpen by remember { mutableStateOf(false) }
    var kaomojiOpen by remember { mutableStateOf(false) }
    var swipeAccum by remember { mutableStateOf(0f) }

    val scheme = MaterialTheme.colorScheme
    val letterBg = if (prefs.accentKeys) scheme.primaryContainer else scheme.surfaceContainerHighest
    val letterPressed = if (prefs.accentKeys) scheme.primary else scheme.surfaceContainerHigh
    val letterContent = if (prefs.accentKeys) scheme.onPrimaryContainer else scheme.onSurface
    val specialBg = scheme.surfaceContainerHigh
    val specialPressed = scheme.surfaceContainerHighest
    val specialContent = scheme.onSurfaceVariant
    val activeBg = scheme.secondaryContainer
    val activeContent = scheme.onSecondaryContainer
    val rowHeight = (48f * prefs.keyHeightScale).dp
    val lettersFont = if (layout == "ru") 17.sp else 19.sp

    val spaceLabel = if (layout == "ru") R.string.lang_name_ru else R.string.lang_name_en

    fun switchLayout() {
        layout = if (layout == "ru") "en" else "ru"
        onLayoutChanged(layout)
    }

    fun handle(key: Key) {
        accentChoices = null
        clipboardOpen = false
        kaomojiOpen = false
        when (key) {
            is Key.Char -> {
                val c = if (layer == Layer.LETTERS && shift != ShiftState.OFF) key.upper else key.base
                onText(c)
                if (shift == ShiftState.ON) shift = ShiftState.OFF
            }
            is Key.Text -> {
                val c = if (shift != ShiftState.OFF) key.upper else key.text
                onText(c)
            }
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
            Key.LayoutSwitch -> switchLayout()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceContainerLow)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 3.dp)
            .padding(top = 2.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedVisibility(
            visible = clipboardOpen,
            enter = fadeIn(spring(stiffness = 400f)) +
                expandVertically(spring(dampingRatio = 0.9f, stiffness = 300f)),
            exit = shrinkVertically(spring(dampingRatio = 0.9f, stiffness = 450f)) + fadeOut(),
        ) {
            ClipboardPanel(
                entries = clipboardEntries,
                onPaste = onClipboardPaste,
                onDelete = onClipboardDelete,
                onClearAll = onClipboardClear,
            )
        }

        AnimatedVisibility(
            visible = kaomojiOpen,
            enter = fadeIn(spring(stiffness = 400f)) +
                expandVertically(spring(dampingRatio = 0.9f, stiffness = 300f)),
            exit = shrinkVertically(spring(dampingRatio = 0.9f, stiffness = 450f)) + fadeOut(),
        ) {
            KaomojiPanel(onInsert = { onText(it) })
        }

        AnimatedVisibility(
            visible = accentChoices != null,
            enter = fadeIn(spring(stiffness = 400f)) +
                expandVertically(spring(dampingRatio = 0.9f, stiffness = 300f)),
            exit = shrinkVertically(spring(dampingRatio = 0.9f, stiffness = 450f)) + fadeOut(),
        ) {
            val choices = accentChoices.orEmpty()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                choices.forEach { c ->
                    KeyCap(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        label = c,
                        fontSize = 18.sp,
                        container = scheme.secondaryContainer,
                        pressedContainer = scheme.secondary,
                        contentColor = scheme.onSecondaryContainer,
                        cornerRadius = 22.dp,
                        elevation = 1.dp,
                        onTap = {
                            onTap()
                            onText(c)
                            accentChoices = null
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarIcon(
                icon = Icons.Outlined.ContentPaste,
                container = if (clipboardOpen) activeBg else Color.Transparent,
                contentColor = if (clipboardOpen) activeContent else specialContent,
                onTap = {
                    onStrongTap()
                    accentChoices = null
                    kaomojiOpen = false
                    clipboardOpen = !clipboardOpen
                },
            )
            ToolbarIcon(
                icon = Icons.Outlined.EmojiEmotions,
                container = if (kaomojiOpen) activeBg else Color.Transparent,
                contentColor = if (kaomojiOpen) activeContent else specialContent,
                onTap = {
                    onStrongTap()
                    accentChoices = null
                    clipboardOpen = false
                    kaomojiOpen = !kaomojiOpen
                },
            )
            suggestions.forEach { s ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onTap()
                            onSuggestion(s)
                        }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = s,
                        color = scheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ToolbarIcon(
                icon = Icons.Outlined.Settings,
                container = Color.Transparent,
                contentColor = specialContent,
                onTap = {
                    onStrongTap()
                    onOpenApp()
                },
            )
        }

        Layouts.rows(layout, layer).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                row.forEachIndexed { colIndex, key ->
                    val entrance = (rowIndex * 30 + colIndex * 6)
                    when (key) {
                        is Key.Char -> {
                            val variants = Diacritics.variants(layout, layer, key.base)
                            KeyCap(
                                modifier = Modifier.weight(key.weight).fillMaxHeight(),
                                label = if (layer == Layer.LETTERS && shift != ShiftState.OFF) key.upper else key.base,
                                container = letterBg,
                                pressedContainer = letterPressed,
                                contentColor = letterContent,
                                fontSize = lettersFont,
                                entranceDelayMillis = entrance,
                                onTap = { onTap(); handle(key) },
                                onLongPress = variants?.let { vs ->
                                    { onStrongTap(); accentChoices = vs }
                                },
                            )
                        }
                        is Key.Text -> {
                            val variants = Diacritics.variants(layout, layer, key.text)
                            KeyCap(
                                modifier = Modifier.weight(key.weight).fillMaxHeight(),
                                label = if (shift != ShiftState.OFF) key.upper else key.text,
                                fontSize = 16.sp,
                                container = specialBg,
                                pressedContainer = specialPressed,
                                contentColor = specialContent,
                                entranceDelayMillis = entrance,
                                onTap = { onTap(); handle(key) },
                                onLongPress = variants?.let { vs ->
                                    { onStrongTap(); accentChoices = vs }
                                },
                            )
                        }
                        Key.Shift -> KeyCap(
                            modifier = Modifier.weight(key.weight).fillMaxHeight(),
                            icon = when (shift) {
                                ShiftState.OFF -> Icons.Outlined.KeyboardArrowUp
                                ShiftState.ON -> Icons.Filled.KeyboardArrowUp
                                ShiftState.LOCKED -> Icons.Filled.KeyboardCapslock
                            },
                            container = if (shift == ShiftState.OFF) specialBg else activeBg,
                            pressedContainer = specialPressed,
                            contentColor = if (shift == ShiftState.OFF) specialContent else activeContent,
                            cornerRadius = 18.dp,
                            border = shift == ShiftState.LOCKED,
                            borderColor = scheme.secondary,
                            entranceDelayMillis = entrance,
                            onTap = { onStrongTap(); handle(key) },
                        )
                        Key.Backspace -> KeyCap(
                            modifier = Modifier.weight(key.weight).fillMaxHeight(),
                            icon = Icons.Filled.Backspace,
                            container = specialBg,
                            pressedContainer = specialPressed,
                            contentColor = specialContent,
                            cornerRadius = 18.dp,
                            entranceDelayMillis = entrance,
                            onTap = { onStrongTap() },
                            repeatable = true,
                            repeatAction = onDelete,
                        )
                        Key.Space -> KeyCap(
                            modifier = Modifier.weight(key.weight).fillMaxHeight(),
                            label = stringResource(spaceLabel),
                            fontSize = 12.sp,
                            container = specialBg,
                            pressedContainer = specialPressed,
                            contentColor = specialContent,
                            cornerRadius = 24.dp,
                            pressedCornerScale = 1.04f,
                            entranceDelayMillis = entrance,
                            onTap = { onTap(); handle(key) },
                            onSwipeHorizontal = { delta ->
                                swipeAccum += delta
                                if (abs(swipeAccum) > 70f) {
                                    swipeAccum = 0f
                                    onStrongTap()
                                    switchLayout()
                                }
                            },
                        )
                        Key.Enter -> KeyCap(
                            modifier = Modifier.weight(key.weight).fillMaxHeight(),
                            label = enterLabel ?: "↵",
                            fontSize = if ((enterLabel?.length ?: 1) > 4) 12.sp else 15.sp,
                            container = scheme.tertiaryContainer,
                            pressedContainer = scheme.tertiary,
                            contentColor = scheme.onTertiaryContainer,
                            cornerRadius = 18.dp,
                            pressedCornerScale = 1.25f,
                            entranceDelayMillis = entrance,
                            onTap = { onStrongTap(); handle(key) },
                        )
                        Key.Symbols -> KeyCap(
                            modifier = Modifier.weight(key.weight).fillMaxHeight(),
                            label = "?123",
                            fontSize = 14.sp,
                            container = specialBg,
                            pressedContainer = specialPressed,
                            contentColor = specialContent,
                            entranceDelayMillis = entrance,
                            onTap = { onStrongTap(); handle(key) },
                        )
                        Key.Letters -> KeyCap(
                            modifier = Modifier.weight(key.weight).fillMaxHeight(),
                            label = "ABC",
                            fontSize = 14.sp,
                            container = specialBg,
                            pressedContainer = specialPressed,
                            contentColor = specialContent,
                            entranceDelayMillis = entrance,
                            onTap = { onStrongTap(); handle(key) },
                        )
                        Key.LayoutSwitch -> KeyCap(
                            modifier = Modifier.weight(key.weight).fillMaxHeight(),
                            icon = Icons.Filled.Language,
                            container = specialBg,
                            pressedContainer = specialPressed,
                            contentColor = specialContent,
                            cornerRadius = 18.dp,
                            entranceDelayMillis = entrance,
                            onTap = { onStrongTap(); handle(key) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarIcon(
    icon: ImageVector,
    container: Color,
    contentColor: Color,
    onTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .clickable { onTap() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ClipboardPanel(
    entries: List<String>,
    onPaste: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (entries.isEmpty()) {
            Text(
                text = stringResource(R.string.clip_empty),
                color = scheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            )
        } else {
            entries.take(5).forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.surfaceContainerHighest)
                        .clickable { onPaste(entry) }
                        .padding(start = 14.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = entry.replace("\n", " ")
                            .let { if (it.length > 80) it.take(80) + "…" else it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 10.dp),
                        color = scheme.onSurface,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDelete(entry) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.clip_clear_all),
                color = scheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onClearAll() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KaomojiPanel(onInsert: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 108.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Kaomoji.ALL.forEach { k ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(scheme.secondaryContainer)
                    .clickable { onInsert(k) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = k,
                    color = scheme.onSecondaryContainer,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@Composable
fun KeyCap(
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector? = null,
    fontSize: TextUnit = 19.sp,
    container: Color,
    pressedContainer: Color,
    contentColor: Color,
    cornerRadius: Dp = 16.dp,
    pressedCornerScale: Float = 1.12f,
    elevation: Dp = 1.dp,
    border: Boolean = false,
    borderColor: Color = Color.Unspecified,
    onTap: () -> Unit = {},
    onLongPress: (() -> Unit)? = null,
    onSwipeHorizontal: ((Float) -> Unit)? = null,
    repeatable: Boolean = false,
    repeatAction: (() -> Unit)? = null,
    entranceDelayMillis: Int = 0,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val corner by animateDpAsState(
        targetValue = if (pressed) cornerRadius * pressedCornerScale else cornerRadius,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f),
        label = "corner",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f),
        label = "scale",
    )
    val bg by animateColorAsState(
        targetValue = if (pressed) pressedContainer else container,
        label = "bg",
    )
    val shade by animateDpAsState(
        targetValue = if (pressed) elevation + 2.dp else elevation,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f),
        label = "shade",
    )

    val appear = remember { Animatable(0.88f) }
    LaunchedEffect(Unit) {
        delay(entranceDelayMillis.toLong())
        appear.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 240f))
    }

    var longFired by remember { mutableStateOf(false) }
    if (onLongPress != null) {
        LaunchedEffect(pressed) {
            if (pressed) {
                longFired = false
                delay(400)
                if (pressed) {
                    longFired = true
                    onLongPress()
                }
            }
        }
    }
    if (repeatable && repeatAction != null) {
        LaunchedEffect(pressed) {
            if (pressed) {
                repeatAction()
                delay(320)
                while (true) {
                    repeatAction()
                    delay(45)
                }
            }
        }
    }

    val cornerShape = RoundedCornerShape(corner)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = appear.value * scale
                scaleY = appear.value * scale
            }
            .shadow(shade, cornerShape)
            .let { m ->
                if (border && borderColor != Color.Unspecified) {
                    m.border(2.dp, borderColor, cornerShape)
                } else m
            }
            .background(bg, cornerShape)
            .clip(cornerShape)
            .let { m ->
                if (onSwipeHorizontal != null) {
                    m.pointerInput(Unit) {
                        detectHorizontalDragGestures { _, amount ->
                            onSwipeHorizontal(amount)
                        }
                    }
                } else m
            }
            .clickable(interactionSource = interaction, indication = null) {
                if (longFired) {
                    longFired = false
                } else {
                    onTap()
                }
            },
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
