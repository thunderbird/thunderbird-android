package net.thunderbird.core.file

import android.content.ContentResolver
import com.eygraber.uri.Uri
import com.eygraber.uri.toAndroidUri
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.asSink
import kotlinx.io.asSource

/**
 * Android implementation of [FileSystemManager] that uses [ContentResolver] to perform file operations.
 */
class AndroidFileSystemManager(
    private val contentResolver: ContentResolver,
) : FileSystemManager {
    override fun openSink(uri: Uri): RawSink? {
        // Use truncate/overwrite mode by default
        return contentResolver.openOutputStream(uri.toAndroidUri(), "wt")?.asSink()
    }

    override fun openSource(uri: Uri): RawSource? {
        return contentResolver.openInputStream(uri.toAndroidUri())?.asSource()
    }
}
