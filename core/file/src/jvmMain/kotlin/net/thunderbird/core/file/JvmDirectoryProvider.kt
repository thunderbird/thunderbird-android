package net.thunderbird.core.file

import com.eygraber.uri.Uri
import com.eygraber.uri.toUri
import java.io.File

/**
 * JVM implementation of [DirectoryProvider].
 *
 * @param appName The name of the application to create directories for.
 */
class JvmDirectoryProvider(
    appName: String,
) : DirectoryProvider {

    private val userHome: String = System.getProperty("user.home")
    private val os: String = System.getProperty("os.name").lowercase()

    private val appDir: File = when {
        os.contains("mac") -> File(userHome, "Library/Application Support/$appName")
        os.contains("win") -> {
            val roaming = System.getenv("APPDATA")
            if (roaming != null) {
                File(roaming, appName)
            } else {
                File(userHome, ".$appName")
            }
        }

        else -> File(userHome, ".$appName")
    }.apply { mkdirs() }

    private val cacheDir = when {
        os.contains("mac") -> File(userHome, "Library/Caches/$appName")
        os.contains("win") -> {
            val localAppData = System.getenv("LOCALAPPDATA")
            if (localAppData != null) {
                File(localAppData, appName)
            } else {
                File(appDir, "cache")
            }
        }

        else -> {
            val xdg = System.getenv("XDG_CACHE_HOME")
            if (xdg != null) {
                File(xdg, appName)
            } else {
                File(userHome, ".cache/$appName")
            }
        }
    }.apply { mkdirs() }

    private val filesDir = File(appDir, "files").apply { mkdirs() }

    override fun getCacheDir(): Uri = cacheDir.toUri()

    override fun getFilesDir(): Uri = filesDir.toUri()
}
