package net.thunderbird.app.composition.core

import net.thunderbird.app.composition.core.configstore.configStoreCompositionModule
import org.koin.dsl.module

internal val coreCompositionModule = module {
    includes(configStoreCompositionModule)
}
