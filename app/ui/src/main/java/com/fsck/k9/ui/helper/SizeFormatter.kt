package com.fsck.k9.ui.helper

import android.content.res.Resources
import com.fsck.k9.ui.R

class SizeFormatter(private val resources: Resources) {
    /*
     * Formats the given size as a String in bytes, kB, MB or GB with a single digit
     * of precision. Ex: 12,315,000 = 12.3 MB
     */
    fun formatSize(size: Long): String {
        if (size > 1024000000) {
            return ((size / 102400000).toFloat() / 10).toString() + resources.getString(R.string.abbrev_gigabytes)
        }
        if (size > 1024000) {
            return ((size / 102400).toFloat() / 10).toString() + resources.getString(R.string.abbrev_megabytes)
        }
        if (size > 1024) {
            return ((size / 102).toFloat() / 10).toString() + resources.getString(R.string.abbrev_kilobytes)
        }

        return size.toString() + resources.getString(R.string.abbrev_bytes)
    }
}
