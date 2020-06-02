package com.fsck.k9.ui.folders

import android.content.Context

class FolderNameFormatterFactory {
    fun create(context: Context): FolderNameFormatter {
        return FolderNameFormatter(context.resources)
    }
}
