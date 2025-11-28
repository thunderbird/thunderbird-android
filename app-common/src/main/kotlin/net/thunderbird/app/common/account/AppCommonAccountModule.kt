package net.thunderbird.app.common.account

import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.feature.account.setup.AccountSetupExternalContract
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import net.thunderbird.app.common.account.data.DefaultAccountProfileLocalDataSource
import net.thunderbird.app.common.account.data.DefaultLegacyAccountManager
import net.thunderbird.core.android.account.AccountDefaultsProvider
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.avatar.AvatarIcon
import net.thunderbird.feature.account.avatar.AvatarIconCatalog
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator
import net.thunderbird.feature.account.avatar.DefaultAvatarIconCatalog
import net.thunderbird.feature.account.avatar.DefaultAvatarMonogramCreator
import net.thunderbird.feature.account.core.AccountCoreExternalContract.AccountProfileLocalDataSource
import net.thunderbird.feature.account.core.featureAccountCoreModule
import net.thunderbird.feature.account.storage.legacy.featureAccountStorageLegacyModule
import net.thunderbird.feature.mail.account.api.AccountManager
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import app.k9mail.core.ui.legacy.theme2.common.R as ThemeCommonR

internal val appCommonAccountModule = module {
    includes(
        featureAccountCoreModule,
        featureAccountStorageLegacyModule,
    )

    single<AccountManager<LegacyAccount>> {
        DefaultLegacyAccountManager(
            accountManager = get(),
            accountDataMapper = get(),
        )
    } binds arrayOf(LegacyAccountManager::class)

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

    factory<ImmutableList<Int>>(named("AccountColors")) {
        androidContext().resources.getIntArray(
            ThemeCommonR.array.account_colors,
        ).toList().toImmutableList()
    }

    factory {
        AccountColorPicker(
            repository = get(),
            accountColors = get(named("AccountColors")),
        )
    }

    single<AvatarIconCatalog<AvatarIcon<ImageVector>>> {
        DefaultAvatarIconCatalog()
    }

    factory<AvatarMonogramCreator> {
        DefaultAvatarMonogramCreator()
    }

    factory<AccountSetupExternalContract.AccountCreator> {
        AccountCreator(
            accountColorPicker = get(),
            localFoldersCreator = get(),
            preferences = get(),
            context = androidApplication(),
            deletePolicyProvider = get(),
            messagingController = get(),
            avatarMonogramCreator = get(),
            unifiedInboxConfigurator = get(),
        )
    }
}
