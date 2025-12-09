package net.thunderbird.app.common.core

import com.eygraber.uri.toAndroidUri
import net.thunderbird.app.common.core.configstore.appCommonCoreConfigStoreModule
import net.thunderbird.app.common.core.logging.appCommonCoreLogger
import net.thunderbird.app.common.core.ui.appCommonCoreUiModule
import net.thunderbird.core.file.AndroidDirectoryProvider
import net.thunderbird.core.file.AndroidFileSystemManager
import net.thunderbird.core.file.AndroidMimeTypeProvider
import net.thunderbird.core.file.AndroidMimeTypeResolver
import net.thunderbird.core.file.DefaultFileManager
import net.thunderbird.core.file.DirectoryProvider
import net.thunderbird.core.file.FileManager
import net.thunderbird.core.file.FileSystemManager
import net.thunderbird.core.file.MimeTypeResolver
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val appCommonCoreModule: Module = module {
    includes(
        appCommonCoreConfigStoreModule,
        appCommonCoreLogger,
        appCommonCoreUiModule,
    )

    single<FileSystemManager> {
        AndroidFileSystemManager(
            contentResolver = androidContext().contentResolver,
        )
    }

    single<FileManager> {
        DefaultFileManager(
            fileSystem = get(),
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
