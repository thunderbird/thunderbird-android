package com.fsck.k9.ui.helper

import android.content.res.Resources
import com.fsck.k9.ui.R

class SizeFormatter(private val resources: Resources) {
    fun formatSize(size: Long): String {
        return when {
            size >= 999_950_000L -> {
                val number = (size / 1_000_000L).toFloat() / 1000f
                resources.getString(R.string.size_format_gigabytes, number)
            }
            size >= 999_950L -> {
                val number = (size / 1000L).toFloat() / 1000f
                resources.getString(R.string.size_format_megabytes, number)
            }
            size >= 1000L -> {
                val number = size.toFloat() / 1000f
                resources.getString(R.string.size_format_kilobytes, number)
            }
            else -> {
                resources.getString(R.string.size_format_bytes, size)
            }
        }
    }
}
