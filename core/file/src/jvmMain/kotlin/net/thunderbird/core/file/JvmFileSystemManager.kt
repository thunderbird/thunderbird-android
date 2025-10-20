package net.thunderbird.core.file

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
    override fun openSink(uriString: String): RawSink? {
        // Only support simple file paths for JVM implementation
        return try {
            val file = File(uriString)
            // create parent directories if necessary
            file.parentFile?.mkdirs()
            val append = false // overwrite/truncate by default
            FileOutputStream(file, append).asSink()
        } catch (_: Throwable) {
            null
        }
    }

    override fun openSource(uriString: String): RawSource? {
        return try {
            val file = File(uriString)
            FileInputStream(file).asSource()
        } catch (_: Throwable) {
            null
        }
    }
}
