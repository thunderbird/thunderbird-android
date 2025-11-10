package net.thunderbird.core.configstore.backend

import java.io.File

/**
 * Implementation of [ConfigBackendFileManager] for JVM platforms.
 *
 * This class provides a way to manage file paths for configuration backends in a JVM environment.
 *
 * @property workDirectory The directory where backend files are stored.
 */
class JvmConfigBackendFileManager(
    private val workDirectory: File,
) : ConfigBackendFileManager {
    override fun getFilePath(backendFileName: String): String {
        return File(workDirectory, backendFileName).absolutePath
    }
}
