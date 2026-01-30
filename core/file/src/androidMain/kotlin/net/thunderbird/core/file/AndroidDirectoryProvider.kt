package net.thunderbird.core.file

import android.content.Context
import androidx.core.net.toUri
import com.eygraber.uri.Uri
import com.eygraber.uri.toKmpUri

/**
 * Android implementation of [DirectoryProvider].
 *
 * @param context The Android context.
 */
class AndroidDirectoryProvider(
    private val context: Context,
) : DirectoryProvider {
    override fun getCacheDir(): Uri {
        return context.cacheDir.toUri().toKmpUri()
    }

    override fun getFilesDir(): Uri {
        return context.filesDir.toUri().toKmpUri()
    }
}
