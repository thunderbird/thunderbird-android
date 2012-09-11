package com.fsck.k9.helper;

import android.content.Context;
import android.os.Build;


/**
 * Helper class to access the system clipboard
 *
 * @see ClipboardManagerApi1
 * @see ClipboardManagerApi11
 */
public abstract class ClipboardManager {
    /**
     * Instance of the API-specific class that interfaces with the clipboard API.
     */
    private static ClipboardManager sInstance = null;

    /**
     * Get API-specific instance of the {@code ClipboardManager} class
     *
     * @param context
     *         A {@link Context} instance.
     *
     * @return Appropriate {@link ClipboardManager} instance for this device.
     */
    public static ClipboardManager getInstance(Context context) {
        Context appContext = context.getApplicationContext();

        if (sInstance == null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                sInstance = new ClipboardManagerApi1(appContext);
            } else {
                sInstance = new ClipboardManagerApi11(appContext);
            }
        }

        return sInstance;
    }


    protected Context mContext;

    /**
     * Constructor
     *
     * @param context
     *         A {@link Context} instance.
     */
    protected ClipboardManager(Context context) {
        mContext = context;
    }

    /**
     * Copy a text string to the system clipboard
     *
     * @param label
     *         User-visible label for the content.
     * @param text
     *         The actual text to be copied to the clipboard.
     */
    public abstract void setText(String label, String text);
}
