package net.thunderbird.core.android.common.keypad

import android.app.Dialog
import android.view.View
import android.view.Window

/** Installs optional keypad handling on an independent window and restores it when the handle is closed. */
fun interface KeypadWindowInstaller {
    fun install(window: Window): AutoCloseable
}

/** Binds to attachment rather than replacing a dialog's show, dismiss, or button listeners. */
object KeypadDialogNavigation {
    @JvmStatic
    fun <T : Dialog> bind(dialog: T, installer: KeypadWindowInstaller): T {
        val window = dialog.window ?: return dialog
        val decor = window.decorView
        val listener = object : View.OnAttachStateChangeListener {
            private var installation: AutoCloseable? = null

            override fun onViewAttachedToWindow(view: View) {
                installation?.close()
                installation = installer.install(window)
            }

            override fun onViewDetachedFromWindow(view: View) {
                installation?.close()
                installation = null
            }
        }
        decor.addOnAttachStateChangeListener(listener)
        if (decor.isAttachedToWindow) listener.onViewAttachedToWindow(decor)
        return dialog
    }
}
