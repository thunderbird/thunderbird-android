package app.k9mail.core.ui.compose.common.keypad

import android.view.Window
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import net.thunderbird.core.android.common.keypad.KeypadWindowInstaller
import org.koin.core.context.GlobalContext

/** Applies the app's optional keypad handling to a Compose dialog's independent window. */
fun Modifier.keypadDialogWindow(): Modifier = this then KeypadDialogWindowElement

private data object KeypadDialogWindowElement : ModifierNodeElement<KeypadDialogWindowNode>() {
    override fun create() = KeypadDialogWindowNode()

    override fun update(node: KeypadDialogWindowNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "keypadDialogWindow"
    }
}

private class KeypadDialogWindowNode : Modifier.Node(), CompositionLocalConsumerModifierNode, ObserverModifierNode {
    private var installedWindow: Window? = null
    private var installation: AutoCloseable? = null

    override fun onAttach() {
        observeWindow()
    }

    override fun onObservedReadsChanged() {
        observeWindow()
    }

    override fun onDetach() {
        closeInstallation()
    }

    private fun observeWindow() {
        observeReads {
            val view = currentValueOf(LocalView)
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window !== installedWindow) {
                closeInstallation()
                installedWindow = window
                // Standalone previews and tests without application DI retain native dialog handling.
                val installer = GlobalContext.getOrNull()?.getOrNull<KeypadWindowInstaller>()
                installation = window?.let { installer?.install(it) }
            }
        }
    }

    private fun closeInstallation() {
        installation?.close()
        installation = null
        installedWindow = null
    }
}
