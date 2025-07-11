package net.thunderbird.core.logging.file

import kotlinx.io.RawSink

/**
 * Android implementation of [FileSystemManager] that uses [ContentResolver] to perform file operations.
 */
class JvmFileSystemManager() : FileSystemManager {
    override fun openSink(uriString: String, mode: String): RawSink? {
        // TODO: Implementation https://github.com/thunderbird/thunderbird-android/issues/9435
        return TODO("Provide the return value")
    }
}
