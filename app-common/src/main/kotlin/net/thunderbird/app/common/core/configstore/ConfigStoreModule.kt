package net.thunderbird.app.common.core.configstore

import net.thunderbird.core.configstore.backend.AndroidConfigBackendFileManager
import net.thunderbird.core.configstore.backend.ConfigBackendFactory
import net.thunderbird.core.configstore.backend.ConfigBackendFileManager
import net.thunderbird.core.configstore.backend.ConfigBackendProvider
import net.thunderbird.core.configstore.backend.DataStoreConfigBackendFactory
import net.thunderbird.core.configstore.backend.DefaultConfigBackendProvider
import org.koin.dsl.module

val appCommonCoreConfigStoreModule = module {

    single<ConfigBackendFileManager> {
        AndroidConfigBackendFileManager(
            context = get(),
        )
    }

    single<ConfigBackendFactory> {
        DataStoreConfigBackendFactory(
            fileManager = get(),
        )
    }

    single<ConfigBackendProvider> {
        DefaultConfigBackendProvider(
            backendFactory = get(),
        )
    }
}
