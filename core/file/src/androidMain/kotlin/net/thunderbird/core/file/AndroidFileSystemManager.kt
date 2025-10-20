package net.thunderbird.core.file

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toUri
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
    override fun openSink(uriString: String): RawSink? {
        val uri: Uri = uriString.toUri()
        // Use truncate/overwrite mode by default
        return contentResolver.openOutputStream(uri, "wt")?.asSink()
    }

    override fun openSource(uriString: String): RawSource? {
        val uri: Uri = uriString.toUri()
        return contentResolver.openInputStream(uri)?.asSource()
    }
}
