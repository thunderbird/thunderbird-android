package net.thunderbird.app.common.core

import net.thunderbird.app.common.appVersion.DefaultAppVersionProvider
import net.thunderbird.app.common.core.configstore.appCommonConfigStoreDiModule
import net.thunderbird.app.common.core.file.appCommonFileDiModule
import net.thunderbird.app.common.core.ui.appCommonCoreUiDiModule
import net.thunderbird.core.common.provider.AppVersionProvider
import net.thunderbird.core.logging.internal.coreLoggingAndroidDiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal val appCommonCoreDiModule: Module = module {
    includes(
        appCommonConfigStoreDiModule,
        appCommonFileDiModule,
        appCommonCoreUiDiModule,
        coreLoggingAndroidDiModule,
    )

    single<AppVersionProvider> { DefaultAppVersionProvider(context = androidContext(), logger = get()) }
}
