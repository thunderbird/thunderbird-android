package net.thunderbird.app.common

import com.fsck.k9.legacyCommonAppModules
import com.fsck.k9.legacyCoreModules
import com.fsck.k9.legacyUiModules
import net.thunderbird.app.common.account.appCommonAccountModule
import net.thunderbird.app.common.feature.appCommonFeatureModule
import net.thunderbird.core.logging.DefaultLogger
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.composite.CompositeLogSink
import org.koin.core.module.Module
import org.koin.dsl.module

val appCommonModule: Module = module {
    includes(legacyCommonAppModules)
    includes(legacyCoreModules)
    includes(legacyUiModules)

    single<LogSink> {
        CompositeLogSink(
            level = LogLevel.VERBOSE,
        )
    }

    single<Logger> {
        DefaultLogger(
            sink = get(),
        )
    }

    includes(
        appCommonAccountModule,
        appCommonFeatureModule,
    )
}
