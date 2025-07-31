package net.thunderbird.core.logging.file

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.io.RawSink
import kotlinx.io.asSink

/**
 * Android implementation of [FileSystemManager] that uses [ContentResolver] to perform file operations.
 */
class AndroidFileSystemManager(
    private val contentResolver: ContentResolver,
) : FileSystemManager {
    override fun openSink(uriString: String, mode: String): RawSink? {
        try {
            val uri: Uri = uriString.toUri()
            return contentResolver.openOutputStream(uri, mode)?.asSink()
        } catch (_: SecurityException) {
//            throw e
        }
        return null
    }
}
