package net.thunderbird.feature.account.avatar.data.datasource

import com.eygraber.uri.Uri
import net.thunderbird.core.file.DirectoryProvider

internal class FakeDirectoryProvider(
    private val baseDir: Uri,
) : DirectoryProvider {
    override fun getCacheDir() = baseDir.buildUpon().appendPath("cache").build()
    override fun getFilesDir() = baseDir.buildUpon().appendPath("files").build()
}
