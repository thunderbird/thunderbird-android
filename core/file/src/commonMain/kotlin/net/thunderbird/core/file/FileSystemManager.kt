package net.thunderbird.core.file

import com.eygraber.uri.Uri
import kotlinx.io.RawSink
import kotlinx.io.RawSource

/**
 * An interface for file system operations that are platform-specific.
 */
interface FileSystemManager {
    /**
     * Opens a sink for writing to a URI.
     *
     * Implementations should open the destination for writing in overwrite/truncate mode.
     *
     * @param uri The URI to open a sink for
     * @return A sink for writing to the URI, or null if the URI couldn't be opened
     */
    fun openSink(uri: Uri): RawSink?

    /**
     * Opens a source for reading from a URI.
     *
     * @param uri The URI to open a source for
     * @return A source for reading from the URI, or null if the URI couldn't be opened
     */
    fun openSource(uri: Uri): RawSource?
}
