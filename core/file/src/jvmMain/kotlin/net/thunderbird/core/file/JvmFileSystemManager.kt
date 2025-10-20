package net.thunderbird.core.file

import com.eygraber.uri.Uri
import com.eygraber.uri.toURI
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.asSink
import kotlinx.io.asSource

/**
 * JVM implementation of [FileSystemManager] using java.io streams.
 */
class JvmFileSystemManager : FileSystemManager {
    override fun openSink(uri: Uri): RawSink? {
        // Only support simple file paths for JVM implementation
        return try {
            val file = File(uri.toURI())
            // create parent directories if necessary
            file.parentFile?.mkdirs()
            val append = false // overwrite/truncate by default
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
}
