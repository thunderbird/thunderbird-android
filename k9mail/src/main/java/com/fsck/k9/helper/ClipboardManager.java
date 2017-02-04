package com.fsck.k9.helper;

import android.content.ClipData;
import android.content.Context;


/**
 * Access the system clipboard using the new {@link ClipboardManager} introduced with API 11
 */
public class ClipboardManager {
    /**
     * Copy a text string to the system clipboard
     *
     * @param label
     *         User-visible label for the content.
     * @param text
     *         The actual text to be copied to the clipboard.
     */
    public static void setText(Context context, String label, String text) {
        android.content.ClipboardManager clipboardManager =
                (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboardManager.setPrimaryClip(clip);
    }
}
