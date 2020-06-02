package com.fsck.k9.helper

import android.content.ClipData
import android.content.Context

/**
 * Access the system clipboard
 */
class ClipboardManager(private val context: Context) {

    /**
     * Copy a text string to the system clipboard
     *
     * @param label User-visible label for the content.
     * @param text The actual text to be copied to the clipboard.
     */
    fun setText(label: String, text: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
    }
}
