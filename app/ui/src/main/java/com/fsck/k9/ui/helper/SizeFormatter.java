package com.fsck.k9.ui.helper;


import android.content.Context;

import com.fsck.k9.ui.R;

public class SizeFormatter {
    /*
     * Formats the given size as a String in bytes, kB, MB or GB with a single digit
     * of precision. Ex: 12,315,000 B = 12.3 MB
     */
    public static String formatSize(Context context, long size) {
        if (size > 1000000000) {
            return ((float)(size / 100000000) / 10) + context.getString(R.string.abbrev_gigabytes);
        }
        if (size > 1000000) {
            return ((float)(size / 100000) / 10) + context.getString(R.string.abbrev_megabytes);
        }
        if (size > 1000) {
            return ((float)(size / 100) / 10) + context.getString(R.string.abbrev_kilobytes);
        }
        return size + context.getString(R.string.abbrev_bytes);
    }

}


