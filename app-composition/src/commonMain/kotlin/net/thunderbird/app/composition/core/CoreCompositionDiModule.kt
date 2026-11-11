package net.thunderbird.app.composition.core

import net.thunderbird.app.composition.core.configstore.configStoreCompositionDiModule
import net.thunderbird.app.composition.core.file.fileCompositionDiModule
import net.thunderbird.app.composition.core.logging.loggingCompositionDiModule
import org.koin.dsl.module

internal val coreCompositionDiModule = module {
    includes(
        configStoreCompositionDiModule,
        fileCompositionDiModule,
        loggingCompositionDiModule,
    )
}
