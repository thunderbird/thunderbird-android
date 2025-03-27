package net.thunderbird.app.common

import app.k9mail.legacy.account.AccountDefaultsProvider
import net.thunderbird.app.common.account.CommonAccountDefaultsProvider
import org.koin.core.module.Module
import org.koin.dsl.module

val appCommonModule: Module = module {
    single<AccountDefaultsProvider> { CommonAccountDefaultsProvider(featureFlagProvider = get()) }
}
