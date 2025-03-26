package net.thunderbird.app.common

import app.k9mail.legacy.account.AccountDefaultsProvider
import app.k9mail.legacy.account.LegacyAccountWrapperManager
import net.thunderbird.app.common.account.CommonAccountDefaultsProvider
import net.thunderbird.app.common.account.data.CommonAccountProfileLocalDataSource
import net.thunderbird.app.common.account.data.CommonLegacyAccountWrapperManager
import net.thunderbird.feature.account.core.AccountCoreExternalContract.AccountProfileLocalDataSource
import net.thunderbird.feature.account.core.featureAccountCoreModule
import org.koin.core.module.Module
import org.koin.dsl.module

val appCommonModule: Module = module {
    includes(
        featureAccountCoreModule,
    )

    single<LegacyAccountWrapperManager> {
        CommonLegacyAccountWrapperManager(
            accountManager = get(),
        )
    }

    single<AccountProfileLocalDataSource> {
        CommonAccountProfileLocalDataSource(
            accountManager = get(),
        )
    }

    single<AccountDefaultsProvider> {
        CommonAccountDefaultsProvider(
            resourceProvider = get(),
            featureFlagProvider = get(),
        )
    }
}
