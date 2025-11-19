package net.thunderbird.app.common.core

import android.content.Context
import net.thunderbird.app.common.core.configstore.appCommonCoreConfigStoreModule
import net.thunderbird.app.common.core.logging.appCommonCoreLogger
import net.thunderbird.app.common.core.ui.appCommonCoreUiModule
import net.thunderbird.core.file.AndroidFileSystemManager
import net.thunderbird.core.file.DefaultFileManager
import net.thunderbird.core.file.FileManager
import net.thunderbird.core.file.FileSystemManager
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
            contentResolver = get<Context>().contentResolver,
        )
    }

    single<FileManager> {
        DefaultFileManager(
            fileSystem = get(),
        )
    }
}
