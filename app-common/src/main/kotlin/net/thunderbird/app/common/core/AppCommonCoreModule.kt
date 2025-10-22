package net.thunderbird.app.common.core

import net.thunderbird.app.common.core.configstore.appCommonCoreConfigStoreModule
import net.thunderbird.app.common.core.logging.appCommonCoreLogger
import net.thunderbird.app.common.core.ui.appCommonCoreUiModule
import org.koin.core.module.Module
import org.koin.dsl.module

val appCommonCoreModule: Module = module {
    includes(
        appCommonCoreConfigStoreModule,
        appCommonCoreLogger,
        appCommonCoreUiModule,
    )
}
