package net.thunderbird.app.common.feature

import app.k9mail.feature.launcher.FeatureLauncherExternalContract
import app.k9mail.feature.launcher.di.featureLauncherModule
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract
import net.thunderbird.feature.notification.impl.inject.featureNotificationModule
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal val appCommonFeatureModule = module {
    includes(featureLauncherModule)
    includes(featureNotificationModule)

    factory<FeatureLauncherExternalContract.AccountSetupFinishedLauncher> {
        AccountSetupFinishedLauncher(
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
