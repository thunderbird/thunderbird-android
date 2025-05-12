package net.thunderbird.app.common

import com.fsck.k9.legacyCommonAppModules
import com.fsck.k9.legacyCoreModules
import com.fsck.k9.legacyUiModules
import net.thunderbird.app.common.account.appCommonAccountModule
import net.thunderbird.app.common.feature.appCommonFeatureModule
import org.koin.core.module.Module
import org.koin.dsl.module

val appCommonModule: Module = module {
    includes(legacyCommonAppModules)
    includes(legacyCoreModules)
    includes(legacyUiModules)

    includes(
        appCommonAccountModule,
        appCommonFeatureModule,
    )
}
