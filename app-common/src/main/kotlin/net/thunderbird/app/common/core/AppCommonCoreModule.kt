package net.thunderbird.app.common.core

import net.thunderbird.app.common.appVersion.DefaultAppVersionProvider
import net.thunderbird.app.common.core.configstore.appCommonConfigStoreModule
import net.thunderbird.app.common.core.file.appCommonFileModule
import net.thunderbird.app.common.core.ui.appCommonCoreUiModule
import net.thunderbird.core.common.provider.AppVersionProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val appCommonCoreModule: Module = module {
    includes(
        appCommonConfigStoreModule,
        appCommonFileModule,
        appCommonCoreUiModule,
    )

    single<AppVersionProvider> { DefaultAppVersionProvider(context = androidContext(), logger = get()) }
}
