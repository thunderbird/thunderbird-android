package net.thunderbird.app.common.core.file

import com.eygraber.uri.toAndroidUri
import net.thunderbird.core.file.AndroidDirectoryProvider
import net.thunderbird.core.file.AndroidFileSystemManager
import net.thunderbird.core.file.AndroidMimeTypeProvider
import net.thunderbird.core.file.AndroidMimeTypeResolver
import net.thunderbird.core.file.DirectoryProvider
import net.thunderbird.core.file.FileSystemManager
import net.thunderbird.core.file.MimeTypeResolver
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal val appCommonFileModule = module {
    single<FileSystemManager> {
        AndroidFileSystemManager(
            contentResolver = androidContext().contentResolver,
        )
    }

    single<DirectoryProvider> {
        AndroidDirectoryProvider(
            context = androidContext(),
        )
    }

    single<AndroidMimeTypeProvider> {
        val contentResolver = androidContext().contentResolver

        AndroidMimeTypeProvider { uri ->
            contentResolver.getType(uri.toAndroidUri())
        }
    }

    single<MimeTypeResolver> {
        AndroidMimeTypeResolver(
            mimeTypeProvider = get(),
        )
    }
}
