package net.thunderbird.core.file

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
     * @param uriString The URI string to open a sink for
     * @return A sink for writing to the URI, or null if the URI couldn't be opened
     */
    fun openSink(uriString: String): RawSink?

    /**
     * Opens a source for reading from a URI.
     *
     * @param uriString The URI string to open a source for
     * @return A source for reading from the URI, or null if the URI couldn't be opened
     */
    fun openSource(uriString: String): RawSource?
}
