package com.fsck.k9.ui.messageview

import android.content.Context
import android.os.Handler
import android.os.Message
import com.fsck.k9.helper.ClipboardManager
import com.fsck.k9.ui.R

internal class LinkTextHandler(
    private val context: Context,
    private val clipboardManager: ClipboardManager,
) : Handler() {

    override fun handleMessage(message: Message) {
        val linkText = message.data.getString("title")?.trim().orEmpty()
        if (linkText.isEmpty()) return

        val label = context.getString(R.string.webview_contextmenu_link_text_clipboard_label)
        clipboardManager.setText(label, linkText)
    }
}
