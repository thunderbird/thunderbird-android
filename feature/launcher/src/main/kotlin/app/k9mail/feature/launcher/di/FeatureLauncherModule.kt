package app.k9mail.feature.launcher.di

import app.k9mail.feature.account.edit.featureAccountEditModule
import app.k9mail.feature.account.setup.featureAccountSetupModule
import org.koin.dsl.module

val featureLauncherModule = module {
    includes(
        featureAccountSetupModule,
        featureAccountEditModule,
    )
}
