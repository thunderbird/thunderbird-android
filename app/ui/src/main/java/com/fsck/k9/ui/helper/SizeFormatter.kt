package com.fsck.k9.ui.helper

import android.content.Context
import com.fsck.k9.ui.R

object SizeFormatter {
    /*
     * Formats the given size as a String in bytes, kB, MB or GB with a single digit
     * of precision. Ex: 12,315,000 = 12.3 MB
     */
    @JvmStatic
    fun formatSize(context: Context, size: Long): String {
        if (size > 1024000000) {
            return ((size / 102400000).toFloat() / 10).toString() + context.getString(R.string.abbrev_gigabytes)
        }
        if (size > 1024000) {
            return ((size / 102400).toFloat() / 10).toString() + context.getString(R.string.abbrev_megabytes)
        }
        if (size > 1024) {
            return ((size / 102).toFloat() / 10).toString() + context.getString(R.string.abbrev_kilobytes)
        }

        return size.toString() + context.getString(R.string.abbrev_bytes)
    }
}
