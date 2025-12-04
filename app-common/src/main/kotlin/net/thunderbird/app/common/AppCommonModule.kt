package net.thunderbird.app.common

import com.fsck.k9.legacyCommonAppModules
import com.fsck.k9.legacyCoreModules
import com.fsck.k9.legacyUiModules
import net.thunderbird.app.common.account.appCommonAccountModule
import net.thunderbird.app.common.appConfig.AndroidPlatformConfigProvider
import net.thunderbird.app.common.core.appCommonCoreModule
import net.thunderbird.app.common.feature.appCommonFeatureModule
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import org.koin.core.module.Module
import org.koin.dsl.module

val appCommonModule: Module = module {
    includes(legacyCommonAppModules)
    includes(legacyCoreModules)
    includes(legacyUiModules)

    includes(
        appCommonAccountModule,
        appCommonCoreModule,
        appCommonFeatureModule,
    )

    single<PlatformConfigProvider> { AndroidPlatformConfigProvider() }
}
