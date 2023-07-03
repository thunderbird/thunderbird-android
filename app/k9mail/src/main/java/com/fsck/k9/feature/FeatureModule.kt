package com.fsck.k9.feature

import app.k9mail.feature.launcher.FeatureLauncherExternalContract
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val featureModule = module {
    factory<FeatureLauncherExternalContract.AccountSetupFinishedLauncher> {
        AccountSetupFinishedLauncher(
            context = androidContext(),
        )
    }
}
