package dev.aezochka.m3ekeyboard.ime

import android.content.ClipboardManager
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.aezochka.m3ekeyboard.MainActivity
import dev.aezochka.m3ekeyboard.R
import dev.aezochka.m3ekeyboard.settings.KeyboardPrefs
import dev.aezochka.m3ekeyboard.settings.KeyboardSettings
import dev.aezochka.m3ekeyboard.ui.KeyboardView
import dev.aezochka.m3ekeyboard.ui.M3EKeyboardTheme
import dev.aezochka.m3ekeyboard.ui.Suggest

class M3EInputMethodService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val viewModelStoreValue = ViewModelStore()

    private var prefsState by mutableStateOf(KeyboardPrefs())
    private var enterLabelState by mutableStateOf<String?>(null)
    private var layoutState by mutableStateOf("en")
    private var suggestionsState by mutableStateOf<List<String>>(emptyList())
    private val clipboardHistory = mutableStateListOf<String>()
    private var selfInputView: View? = null
    private var clipboardManager: ClipboardManager? = null

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener { onClipboardChanged() }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = viewModelStoreValue

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        val p = KeyboardSettings.read(this)
        prefsState = p
        layoutState = p.layout
        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipboardListener)
    }

    override fun onDestroy() {
        clipboardManager?.removePrimaryClipChangedListener(clipboardListener)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStoreValue.clear()
        super.onDestroy()
    }

    override fun onWindowShown() {
        super.onWindowShown()
        if (!lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        prefsState = KeyboardSettings.read(this)
        layoutState = prefsState.layout
        enterLabelState = enterActionLabel(info)
        updateSuggestions()
    }

    override fun onCreateInputView(): View {
        val view = ImeComposeView(this)
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycle))
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeViewModelStoreOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
        }
        selfInputView = view
        return view
    }

    private inner class ImeComposeView(context: android.content.Context) : AbstractComposeView(context) {
        @Composable
        override fun Content() {
            M3EKeyboardTheme(dynamic = prefsState.dynamicColors) {
                KeyboardView(
                    prefs = prefsState,
                    initialLayout = layoutState,
                    enterLabel = enterLabelState,
                    suggestions = suggestionsState,
                    clipboardEntries = clipboardHistory,
                    onText = { text -> commitKey(text) },
                    onDelete = { deleteBeforeCursor() },
                    onEnter = { sendEnter() },
                    onTap = { tapFeedback(false) },
                    onStrongTap = { tapFeedback(true) },
                    onSuggestion = { word -> commitSuggestion(word) },
                    onClipboardPaste = { text -> pasteClipboard(text) },
                    onClipboardClear = { clipboardHistory.clear() },
                    onOpenApp = { launchApp() },
                    onLayoutChanged = { layout -> saveLayout(layout) },
                )
            }
        }
    }

    private fun commitKey(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        updateSuggestions()
    }

    private fun deleteBeforeCursor() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (selected.isNullOrEmpty()) {
            if (!ic.deleteSurroundingText(1, 0)) {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            }
        } else {
            ic.commitText("", 1)
        }
        updateSuggestions()
    }

    private fun sendEnter() {
        val ic = currentInputConnection ?: return
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
            ?: EditorInfo.IME_ACTION_NONE
        if (action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        } else {
            ic.performEditorAction(action)
        }
        updateSuggestions()
    }

    private fun commitSuggestion(word: String) {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(48, 0)?.toString() ?: ""
        val prefix = before.takeLastWhile { it.isLetter() }
        ic.beginBatchEdit()
        if (prefix.isNotEmpty()) {
            ic.deleteSurroundingText(prefix.length, 0)
        }
        ic.commitText(word, 1)
        ic.commitText(" ", 1)
        ic.endBatchEdit()
        updateSuggestions()
    }

    private fun pasteClipboard(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        updateSuggestions()
    }

    private fun launchApp() {
        try {
            val intent = Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private fun tapFeedback(strong: Boolean) {
        if (prefsState.haptics) {
            val constant = if (strong) HapticFeedbackConstants.CONTEXT_CLICK else HapticFeedbackConstants.KEYBOARD_TAP
            selfInputView?.performHapticFeedback(constant)
        }
        if (prefsState.sound) {
            (getSystemService(AUDIO_SERVICE) as? AudioManager)?.playSoundEffect(AudioManager.FX_KEY_CLICK, -1f)
        }
    }

    private fun saveLayout(layout: String) {
        layoutState = layout
        KeyboardSettings.write(this, prefsState.copy(layout = layout))
    }

    private fun updateSuggestions() {
        val ic = currentInputConnection
        if (ic == null) {
            suggestionsState = emptyList()
            return
        }
        val before = ic.getTextBeforeCursor(48, 0)?.toString() ?: ""
        val word = before.takeLastWhile { it.isLetter() }
        suggestionsState = if (word.length >= 2) {
            Suggest.suggest(word, layoutState)
        } else {
            emptyList()
        }
    }

    private fun onClipboardChanged() {
        try {
            val text = clipboardManager?.primaryClip?.getItemAt(0)
                ?.coerceToText(this)?.toString()?.trim()
            if (!text.isNullOrEmpty() && text.length <= 2000 && clipboardHistory.firstOrNull() != text) {
                clipboardHistory.add(0, text)
                while (clipboardHistory.size > 10) {
                    clipboardHistory.removeAt(clipboardHistory.size - 1)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun enterActionLabel(info: EditorInfo?): String? = when (info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)) {
        EditorInfo.IME_ACTION_GO -> getString(R.string.action_go)
        EditorInfo.IME_ACTION_SEARCH -> getString(R.string.action_search)
        EditorInfo.IME_ACTION_SEND -> getString(R.string.action_send)
        EditorInfo.IME_ACTION_DONE -> getString(R.string.action_done)
        EditorInfo.IME_ACTION_NEXT, EditorInfo.IME_ACTION_PREVIOUS -> getString(R.string.action_next)
        else -> null
    }
}
