package net.thunderbird.app.composition.core.configstore

import net.thunderbird.core.configstore.backend.ConfigBackendFactory
import net.thunderbird.core.configstore.backend.ConfigBackendProvider
import net.thunderbird.core.configstore.backend.DataStoreConfigBackendFactory
import net.thunderbird.core.configstore.backend.DefaultConfigBackendProvider
import org.koin.dsl.module

internal val configStoreCompositionModule = module {
    single<ConfigBackendFactory> { DataStoreConfigBackendFactory(fileManager = get()) }
    single<ConfigBackendProvider> { DefaultConfigBackendProvider(backendFactory = get()) }
}
