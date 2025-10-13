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
     * @param uriString The URI string to open a sink for
     * @param mode The mode to open the sink in (e.g., "wt" for write text)
     * @return A sink for writing to the URI, or null if the URI couldn't be opened
     */
    fun openSink(uriString: String, mode: String): RawSink?

    /**
     * Opens a source for reading from a URI.
     *
     * @param uriString The URI string to open a source for
     * @return A source for reading from the URI, or null if the URI couldn't be opened
     */
    fun openSource(uriString: String): RawSource?
}
