package net.thunderbird.app.common.account

import app.k9mail.feature.account.setup.AccountSetupExternalContract
import net.thunderbird.app.common.account.data.DefaultAccountProfileLocalDataSource
import net.thunderbird.app.common.account.data.DefaultLegacyAccountWrapperManager
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.LegacyAccountWrapperManager
import net.thunderbird.feature.account.core.AccountCoreExternalContract.AccountProfileLocalDataSource
import net.thunderbird.feature.account.core.featureAccountCoreModule
import net.thunderbird.feature.account.storage.legacy.featureAccountStorageLegacyModule
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

internal val appCommonAccountModule = module {
    includes(
        featureAccountCoreModule,
        featureAccountStorageLegacyModule,
    )

    single<LegacyAccountWrapperManager> {
        DefaultLegacyAccountWrapperManager(
            accountManager = get(),
            accountDataMapper = get(),
        )
    }

    single<AccountProfileLocalDataSource> {
        DefaultAccountProfileLocalDataSource(
            accountManager = get(),
            dataMapper = get(),
        )
    }

    single<AccountDefaultsProvider> {
        DefaultAccountDefaultsProvider(
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
