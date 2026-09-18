package net.thunderbird.app.common.core.configstore

import net.thunderbird.core.configstore.backend.AndroidConfigBackendFileManager
import net.thunderbird.core.configstore.backend.ConfigBackendFileManager
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

internal val appCommonConfigStoreModule = module {
    single<ConfigBackendFileManager> {
        AndroidConfigBackendFileManager(
            context = androidApplication(),
        )
    }
}
