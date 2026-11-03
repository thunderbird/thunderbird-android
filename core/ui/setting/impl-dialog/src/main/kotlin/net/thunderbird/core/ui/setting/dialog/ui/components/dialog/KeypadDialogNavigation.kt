package net.thunderbird.core.ui.setting.dialog.ui.components.dialog

import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent

internal val LocalKeypadNavigationEnabled = staticCompositionLocalOf<() -> Boolean> { { false } }

/** Reads focus and input state from the dialog's modifier node, rather than its activity. */
internal fun Modifier.keypadDialogNavigation(
    onDismissRequest: () -> Unit,
    isTextInputFocused: Boolean,
): Modifier = this then KeypadDialogNavigationElement(onDismissRequest, isTextInputFocused)

private data class KeypadDialogNavigationElement(
    val onDismissRequest: () -> Unit,
    val isTextInputFocused: Boolean,
) : ModifierNodeElement<KeypadDialogNavigationNode>() {
    override fun create() = KeypadDialogNavigationNode(onDismissRequest, isTextInputFocused)

    override fun update(node: KeypadDialogNavigationNode) {
        node.onDismissRequest = onDismissRequest
        node.isTextInputFocused = isTextInputFocused
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "keypadDialogNavigation"
        properties["isTextInputFocused"] = isTextInputFocused
    }
}

private class KeypadDialogNavigationNode(
    var onDismissRequest: () -> Unit,
    var isTextInputFocused: Boolean,
) : Modifier.Node(), KeyInputModifierNode, CompositionLocalConsumerModifierNode, ObserverModifierNode {
    private val handler = KeypadDialogKeyHandler()

    override fun onAttach() {
        observeWindowFocus()
    }

    override fun onDetach() {
        handler.reset()
    }

    override fun onObservedReadsChanged() {
        observeWindowFocus()
    }

    private fun observeWindowFocus() {
        observeReads {
            if (!currentValueOf(LocalWindowInfo).isWindowFocused) handler.reset()
        }
    }

    override fun onPreKeyEvent(event: ComposeKeyEvent): Boolean {
        val context = currentValueOf(LocalContext)
        val view = currentValueOf(LocalView)
        val inputMethodManager = context.getSystemService(InputMethodManager::class.java)
        val editorFocused = isTextInputFocused ||
            view.findFocus()?.onCheckIsTextEditor() == true || inputMethodManager.isAcceptingText
        return handler.dispatchKeyEvent(
            event.nativeKeyEvent,
            currentValueOf(LocalKeypadNavigationEnabled)(),
            editorFocused,
            ::performAction,
        )
    }

    override fun onKeyEvent(event: ComposeKeyEvent): Boolean = false

    private fun performAction(action: KeypadDialogAction) {
        val focusManager = currentValueOf(LocalFocusManager)
        when (action) {
            KeypadDialogAction.DISMISS -> {
                val view = currentValueOf(LocalView)
                val imeVisible = ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.ime()) == true
                if (imeVisible) currentValueOf(LocalSoftwareKeyboardController)?.hide() else onDismissRequest()
            }

            KeypadDialogAction.PREVIOUS -> focusManager.moveFocus(FocusDirection.Previous)

            KeypadDialogAction.NEXT -> focusManager.moveFocus(FocusDirection.Next)
        }
    }
}

internal enum class KeypadDialogAction { DISMISS, PREVIOUS, NEXT }

internal class KeypadDialogKeyHandler {
    private val pending = mutableMapOf<Pair<Int, Int>, KeypadDialogAction>()

    fun dispatchKeyEvent(
        event: KeyEvent,
        enabled: Boolean,
        editorFocused: Boolean,
        performAction: (KeypadDialogAction) -> Unit,
    ): Boolean = when (event.action) {
        KeyEvent.ACTION_UP -> onKeyReleased(event, enabled, performAction)
        KeyEvent.ACTION_DOWN -> onKeyPressed(event, enabled, editorFocused)
        else -> false
    }

    private fun onKeyReleased(
        event: KeyEvent,
        enabled: Boolean,
        performAction: (KeypadDialogAction) -> Unit,
    ): Boolean {
        val key = event.deviceId to event.keyCode
        val action = pending.remove(key) ?: return false
        if (enabled && !event.isCanceled && event.hasNoModifiers()) performAction(action)
        return true
    }

    private fun onKeyPressed(event: KeyEvent, enabled: Boolean, editorFocused: Boolean): Boolean {
        val key = event.deviceId to event.keyCode
        if (key in pending) return true
        val action = if (enabled && event.hasNoModifiers() && event.repeatCount == 0) {
            findAction(event.keyCode, editorFocused)
        } else {
            null
        }
        return if (action != null) {
            pending[key] = action
            true
        } else {
            false
        }
    }

    private fun findAction(keyCode: Int, editorFocused: Boolean): KeypadDialogAction? =
        when (keyCode) {
            KeyEvent.KEYCODE_F1, KeyEvent.KEYCODE_SOFT_LEFT -> KeypadDialogAction.NEXT
            KeyEvent.KEYCODE_F2, KeyEvent.KEYCODE_SOFT_RIGHT -> KeypadDialogAction.DISMISS
            KeyEvent.KEYCODE_STAR -> if (editorFocused) null else KeypadDialogAction.PREVIOUS
            KeyEvent.KEYCODE_POUND -> if (editorFocused) null else KeypadDialogAction.NEXT
            else -> null
        }

    fun reset() {
        pending.clear()
    }
}
