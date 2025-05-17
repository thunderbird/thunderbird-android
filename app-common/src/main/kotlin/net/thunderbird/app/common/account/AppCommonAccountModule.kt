package net.thunderbird.app.common.account

import app.k9mail.feature.account.setup.AccountSetupExternalContract
import net.thunderbird.app.common.account.data.CommonAccountProfileLocalDataSource
import net.thunderbird.app.common.account.data.CommonLegacyAccountWrapperManager
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.LegacyAccountWrapperManager
import net.thunderbird.feature.account.core.AccountCoreExternalContract.AccountProfileLocalDataSource
import net.thunderbird.feature.account.core.featureAccountCoreModule
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

internal val appCommonAccountModule = module {
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

    factory {
        AccountColorPicker(
            accountManager = get(),
            resources = get(),
        )
    }

    factory<AccountSetupExternalContract.AccountCreator> {
        AccountCreator(
            accountColorPicker = get(),
            localFoldersCreator = get(),
            preferences = get(),
            context = androidApplication(),
            deletePolicyProvider = get(),
            messagingController = get(),
            unifiedInboxConfigurator = get(),
        )
    }
}
