package net.thunderbird.core.configstore.backend

import android.content.Context

/**
 * Android implementation of [ConfigBackendFileManager] that provides file paths for configuration backends.
 *
 * @param context The Android context used to access application-specific files.
 */
class AndroidConfigBackendFileManager(
    val context: Context,
) : ConfigBackendFileManager {
    override fun getFilePath(backendFileName: String): String {
        return context.filesDir.resolve(backendFileName).absolutePath
    }
}
