package net.thunderbird.core.file

import com.eygraber.uri.Uri

/**
 * An interface to provide platform-specific directories.
 */
interface DirectoryProvider {

    /**
     * Get the cache directory URI.
     *
     * @return The [Uri] of the cache directory.
     */
    fun getCacheDir(): Uri

    /**
     * Get the files directory URI.
     *
     * @return The [Uri] of the files directory.
     */
    fun getFilesDir(): Uri
}
