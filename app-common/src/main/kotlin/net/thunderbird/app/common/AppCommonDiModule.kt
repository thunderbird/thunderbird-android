package net.thunderbird.app.common

import com.fsck.k9.legacyCommonDiModule
import com.fsck.k9.legacyCoreDiModule
import com.fsck.k9.legacyUiDiModule
import net.thunderbird.app.common.account.appCommonAccountDiModule
import net.thunderbird.app.common.activity.DefaultActivityProvider
import net.thunderbird.app.common.appConfig.AndroidPlatformConfigProvider
import net.thunderbird.app.common.core.appCommonCoreDiModule
import net.thunderbird.app.common.feature.appCommonFeatureDiModule
import net.thunderbird.app.common.startup.appCommonStartupDiModule
import net.thunderbird.app.composition.appCompositionDiModule
import net.thunderbird.core.android.common.activity.ActivityProvider
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.module

val appCommonDiModule: Module = module {
    includes(legacyCommonDiModule)
    includes(legacyCoreDiModule)
    includes(legacyUiDiModule)

    includes(
        appCompositionDiModule,
        appCommonAccountDiModule,
        appCommonCoreDiModule,
        appCommonFeatureDiModule,
        appCommonStartupDiModule,
    )

    single<PlatformConfigProvider> { AndroidPlatformConfigProvider() }
    single<ActivityProvider>(createdAtStart = true) {
        DefaultActivityProvider(
            application = androidApplication(),
            logger = get(),
        )
    }
}
