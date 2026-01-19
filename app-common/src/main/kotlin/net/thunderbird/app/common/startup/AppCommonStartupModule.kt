package net.thunderbird.app.common.startup

import net.thunderbird.core.android.common.startup.DatabaseUpgradeInterceptor
import org.koin.core.module.Module
import org.koin.dsl.module

val appCommonStartupModule: Module = module {
    single<DatabaseUpgradeInterceptor> { DefaultDatabaseUpgradeInterceptor() }
}
