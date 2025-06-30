package net.thunderbird.app.common.feature

import app.k9mail.feature.launcher.FeatureLauncherExternalContract
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val featureLauncherModule = module {
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
