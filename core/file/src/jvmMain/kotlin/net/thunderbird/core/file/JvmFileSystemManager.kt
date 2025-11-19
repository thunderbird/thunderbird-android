package net.thunderbird.core.file

import com.eygraber.uri.Uri
import com.eygraber.uri.toURI
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.io.IOException
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.asSink
import kotlinx.io.asSource

/**
 * JVM implementation of [FileSystemManager] using java.io streams.
 */
class JvmFileSystemManager : FileSystemManager {
    override fun openSink(uri: Uri, mode: WriteMode): RawSink? {
        // Only support simple file paths for JVM implementation
        return try {
            val file = File(uri.toURI())
            // create parent directories if necessary
            file.parentFile?.mkdirs()
            val append = when (mode) {
                WriteMode.Truncate -> false
                WriteMode.Append -> true
            }
            FileOutputStream(file, append).asSink()
        } catch (_: Throwable) {
            null
        }
    }

    override fun openSource(uri: Uri): RawSource? {
        return try {
            val file = File(uri.toURI())
            FileInputStream(file).asSource()
        } catch (_: Throwable) {
            null
        }
    }

    override fun delete(uri: Uri) {
        try {
            val file = File(uri.toURI())
            if (!file.delete() && file.exists()) {
                throw IOException("Unable to delete file at: $uri")
            }
        } catch (error: Exception) {
            throw IOException("Unable to delete file at: $uri", error)
        }
    }

    override fun createDirectories(uri: Uri) {
        try {
            val file = File(uri.toURI())
            if (file.exists()) return
            if (!file.mkdirs()) {
                if (!file.exists()) {
                    throw IOException("Unable to create directories at: $uri")
                }
            }
        } catch (error: Exception) {
            throw IOException("Unable to create directories at: $uri", error)
        }
    }
}
