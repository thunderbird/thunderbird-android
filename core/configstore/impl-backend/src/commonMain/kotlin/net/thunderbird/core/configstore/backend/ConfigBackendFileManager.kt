package net.thunderbird.core.configstore.backend

/**
 * Interface for managing file paths for configuration backends.
 *
 * This interface provides a method to retrieve the file path for a given backend file name.
 */
interface ConfigBackendFileManager {

    /**
     * Retrieves the file path for a given backend file name.
     *
     * @param backendFileName The name of the backend file for which to retrieve the path.
     * @return The file path as a [String].
     */
    fun getFilePath(backendFileName: String): String
}
