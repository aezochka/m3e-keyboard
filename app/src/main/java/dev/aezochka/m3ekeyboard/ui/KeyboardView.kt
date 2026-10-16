@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package dev.aezochka.m3ekeyboard.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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

private enum class AreaMode { KEYS, MENU, ACCENT }
private enum class MenuTab { CLIPBOARD, KAOMOJI }

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
    onEditAction: (Int) -> Unit = {},
    onOpenApp: () -> Unit = {},
    onLayoutChanged: (String) -> Unit,
) {
    var layout by remember { mutableStateOf(initialLayout) }
    var layer by remember { mutableStateOf(Layer.LETTERS) }
    var shift by remember { mutableStateOf(ShiftState.OFF) }
    var accentChoices by remember { mutableStateOf<List<String>?>(null) }
    var areaMode by remember { mutableStateOf(AreaMode.KEYS) }
    var swipeAccum by remember { mutableStateOf(0f) }
    var entranceArmed by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(700)
        entranceArmed = false
    }

    val scheme = MaterialTheme.colorScheme
    val letterBg = if (prefs.accentKeys) scheme.primaryContainer else scheme.surfaceContainerHighest
    val letterPressed = if (prefs.accentKeys) scheme.primary else scheme.surfaceContainerLow
    val letterContent = if (prefs.accentKeys) scheme.onPrimaryContainer else scheme.onSurface
    val specialBg = scheme.surfaceContainerHigh
    val specialPressed = scheme.surfaceContainerHighest
    val specialContent = scheme.onSurfaceVariant
    val activeBg = scheme.secondaryContainer
    val activeContent = scheme.onSecondaryContainer
    val rowHeight = (48f * prefs.keyHeightScale).dp
    val lettersFont = if (layout == "ru") 17.sp else 19.sp

    val digitsH = if (prefs.digitsRow) 48.dp else 0.dp
    val fixedHeight = 46.dp + 8.dp + (rowHeight * 4f) + 24.dp + digitsH + 6.dp

    val spaceLabel = if (layout == "ru") R.string.lang_name_ru else R.string.lang_name_en

    fun switchLayout() {
        layout = if (layout == "ru") "en" else "ru"
        onLayoutChanged(layout)
    }

    fun handle(key: Key) {
        accentChoices = null
        if (areaMode == AreaMode.ACCENT) areaMode = AreaMode.KEYS
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
            .height(fixedHeight),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            contentAlignment = Alignment.Center,
        ) {
        HorizontalFloatingToolbar(
            expanded = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ToolbarIcon(
                icon = Icons.Outlined.Menu,
                container = if (areaMode == AreaMode.MENU) activeBg else Color.Transparent,
                contentColor = if (areaMode == AreaMode.MENU) activeContent else specialContent,
                onTap = {
                    onStrongTap()
                    accentChoices = null
                    areaMode = if (areaMode == AreaMode.MENU) AreaMode.KEYS else AreaMode.MENU
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
        }

        AnimatedContent(
            targetState = areaMode,
            modifier = Modifier.fillMaxWidth().weight(1f),
            transitionSpec = {
                val enter = fadeIn(spring(dampingRatio = 0.9f, stiffness = 300f)) +
                    slideInVertically(spring(dampingRatio = 0.9f, stiffness = 300f)) { it / 8 }
                val exit = fadeOut(spring(dampingRatio = 0.9f, stiffness = 400f)) +
                    slideOutVertically(spring(dampingRatio = 0.9f, stiffness = 400f)) { it / 10 }
                enter togetherWith exit
            },
            label = "area",
        ) { mode ->
            when (mode) {
                AreaMode.MENU -> MenuPanel(
                    clipboardEntries = clipboardEntries,
                    onEditAction = onEditAction,
                    onClipboardPaste = onClipboardPaste,
                    onClipboardDelete = onClipboardDelete,
                    onClipboardClear = onClipboardClear,
                    onKaomoji = { onText(it) },
                )
                AreaMode.ACCENT -> AccentPanel(
                    choices = accentChoices.orEmpty(),
                    onPick = { c ->
                        onStrongTap()
                        onText(c)
                        accentChoices = null
                        areaMode = AreaMode.KEYS
                    },
                )
                AreaMode.KEYS -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (prefs.digitsRow) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { d ->
                                KeyCap(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    label = d,
                                    fontSize = 16.sp,
                                    container = specialBg,
                                    pressedContainer = specialPressed,
                                    contentColor = specialContent,
                                    cornerRadius = 14.dp,
                                    onTap = { onTap(); onText(d) },
                                )
                            }
                        }
                    }
                    Layouts.rows(layout, layer).forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeight),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            row.forEachIndexed { colIndex, key ->
                                val entrance = if (entranceArmed) (rowIndex * 30 + colIndex * 6) else 0
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
                                            entranceEnabled = entranceArmed,
                                            onTap = { onTap(); handle(key) },
                                            onLongPress = variants?.let { vs ->
                                                {
                                                    onStrongTap()
                                                    accentChoices = vs
                                                    areaMode = AreaMode.ACCENT
                                                }
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
                                            entranceEnabled = entranceArmed,
                                            onTap = { onTap(); handle(key) },
                                            onLongPress = variants?.let { vs ->
                                                {
                                                    onStrongTap()
                                                    accentChoices = vs
                                                    areaMode = AreaMode.ACCENT
                                                }
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
                                        entranceEnabled = entranceArmed,
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
                                        entranceEnabled = entranceArmed,
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
                                        staticShape = MaterialShapes.Pill.toShape(),
                                        entranceDelayMillis = entrance,
                                        entranceEnabled = entranceArmed,
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
                                        pressedCornerScale = 1.2f,
                                        staticShape = MaterialShapes.Pill.toShape(),
                                        entranceDelayMillis = entrance,
                                        entranceEnabled = entranceArmed,
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
                                        entranceEnabled = entranceArmed,
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
                                        entranceEnabled = entranceArmed,
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
                                        entranceEnabled = entranceArmed,
                                        onTap = { onStrongTap(); handle(key) },
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
private fun AccentPanel(choices: List<String>, onPick: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            choices.forEach { c ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(scheme.secondaryContainer)
                        .clickable { onPick(c) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = c,
                        color = scheme.onSecondaryContainer,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.accent_hint),
            color = scheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuPanel(
    clipboardEntries: List<String>,
    onEditAction: (Int) -> Unit,
    onClipboardPaste: (String) -> Unit,
    onClipboardDelete: (String) -> Unit,
    onClipboardClear: () -> Unit,
    onKaomoji: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var tab by remember { mutableStateOf(MenuTab.CLIPBOARD) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionButton(
                icon = Icons.Outlined.ContentCopy,
                label = stringResource(R.string.act_copy),
                modifier = Modifier.weight(1f),
            ) { onEditAction(android.R.id.copy) }
            ActionButton(
                icon = Icons.Outlined.ContentCut,
                label = stringResource(R.string.act_cut),
                modifier = Modifier.weight(1f),
            ) { onEditAction(android.R.id.cut) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionButton(
                icon = Icons.Outlined.ContentPaste,
                label = stringResource(R.string.act_paste),
                modifier = Modifier.weight(1f),
            ) { onEditAction(android.R.id.paste) }
            ActionButton(
                icon = Icons.Outlined.SelectAll,
                label = stringResource(R.string.act_select_all),
                modifier = Modifier.weight(1f),
            ) { onEditAction(android.R.id.selectAll) }
        }

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = tab == MenuTab.CLIPBOARD,
                onClick = { tab = MenuTab.CLIPBOARD },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text(stringResource(R.string.menu_clipboard)) }
            SegmentedButton(
                selected = tab == MenuTab.KAOMOJI,
                onClick = { tab = MenuTab.KAOMOJI },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text(stringResource(R.string.menu_kaomoji)) }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                MenuTab.CLIPBOARD -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (clipboardEntries.isEmpty()) {
                        Text(
                            stringResource(R.string.clip_empty),
                            color = scheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(10.dp),
                        )
                    } else {
                        clipboardEntries.forEach { entry -> ClipRow(entry, onClipboardPaste, onClipboardDelete) }
                        Text(
                            text = stringResource(R.string.clip_clear_all),
                            color = scheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onClipboardClear() }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                }
                MenuTab.KAOMOJI -> KaomojiFlow(onKaomoji)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KaomojiFlow(onInsert: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    FlowRow(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Kaomoji.ALL.forEach { k ->
            Box(
                modifier = Modifier
                    .clip(MaterialShapes.Pill.toShape())
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
private fun ClipRow(
    entry: String,
    onPaste: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
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

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceContainerHighest)
            .clickable { onTap() }
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            color = scheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
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
    pressedCornerScale: Float = 1.2f,
    staticShape: Shape? = null,
    border: Boolean = false,
    borderColor: Color = Color.Unspecified,
    onTap: () -> Unit = {},
    onLongPress: (() -> Unit)? = null,
    onSwipeHorizontal: ((Float) -> Unit)? = null,
    repeatable: Boolean = false,
    repeatAction: (() -> Unit)? = null,
    entranceDelayMillis: Int = 0,
    entranceEnabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val corner by animateDpAsState(
        targetValue = if (pressed) cornerRadius * pressedCornerScale else cornerRadius,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "corner",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "scale",
    )
    val bg by animateColorAsState(
        targetValue = if (pressed) pressedContainer else container,
        label = "bg",
    )

    val appear = remember { Animatable(if (entranceEnabled) 0.9f else 1f) }
    if (entranceEnabled) {
        LaunchedEffect(Unit) {
            delay(entranceDelayMillis.toLong())
            appear.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 250f))
        }
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

    val cornerShape = staticShape ?: RoundedCornerShape(corner)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = appear.value * scale
                scaleY = appear.value * scale
            }
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
