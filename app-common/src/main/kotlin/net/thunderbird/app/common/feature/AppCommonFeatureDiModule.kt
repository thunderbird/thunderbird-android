package net.thunderbird.app.common.feature

import app.k9mail.feature.launcher.FeatureLauncherExternalContract
import app.k9mail.feature.launcher.featureLauncherDiModule
import net.thunderbird.app.common.feature.account.appCommonFeatureAccountDiModule
import net.thunderbird.app.common.feature.funding.appCommonFeatureFundingDiModule
import net.thunderbird.app.common.feature.mail.appCommonFeatureMailDiModule
import net.thunderbird.feature.account.avatar.featureAccountAvatarDiModule
import net.thunderbird.feature.mail.message.composer.internal.featureMailMessageComposerDiModule
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract
import net.thunderbird.feature.thundermail.internal.common.featureThundermailCommonDiModule
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal val appCommonFeatureDiModule = module {
    includes(appCommonFeatureMailDiModule)
    includes(appCommonFeatureAccountDiModule)
    includes(appCommonFeatureFundingDiModule)
    includes(featureAccountAvatarDiModule)
    includes(featureLauncherDiModule)
    includes(featureMailMessageComposerDiModule)
    includes(featureThundermailCommonDiModule)

    factory<FeatureLauncherExternalContract.MessageListLauncher> {
        MessageListLauncher(
            context = androidContext(),
        )
    }

    single<NavigationDrawerExternalContract.DrawerConfigLoader> {
        NavigationDrawerConfigLoader(get())
    }

    single<NavigationDrawerExternalContract.DrawerConfigWriter> {
        NavigationDrawerConfigWriter(get())
    }
}
