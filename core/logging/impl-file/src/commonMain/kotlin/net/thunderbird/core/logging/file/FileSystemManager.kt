package net.thunderbird.core.logging.file

import kotlinx.io.RawSink

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
}
