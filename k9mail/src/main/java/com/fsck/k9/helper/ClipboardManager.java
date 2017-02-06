package com.fsck.k9.helper;

import android.content.ClipData;
import android.content.Context;


/**
 * Access the system clipboard using the new {@link ClipboardManager} introduced with API 11
 */
public class ClipboardManager {
    public static ClipboardManager getInstance(Context context) {
        Context appContext = context.getApplicationContext();
        return new ClipboardManager(appContext);
    }


    private Context context;

    private ClipboardManager(Context context) {
        this.context = context;
    }

    /**
     * Copy a text string to the system clipboard
     *
     * @param label
     *         User-visible label for the content.
     * @param text
     *         The actual text to be copied to the clipboard.
     */
    public void setText(String label, String text) {
        android.content.ClipboardManager clipboardManager =
                (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboardManager.setPrimaryClip(clip);
    }
}
