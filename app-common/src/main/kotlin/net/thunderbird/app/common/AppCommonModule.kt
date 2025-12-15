package net.thunderbird.app.common

import com.fsck.k9.K9
import com.fsck.k9.legacyCommonAppModules
import com.fsck.k9.legacyCoreModules
import com.fsck.k9.legacyUiModules
import net.thunderbird.app.common.account.appCommonAccountModule
import net.thunderbird.app.common.appConfig.AndroidPlatformConfigProvider
import net.thunderbird.app.common.core.appCommonCoreModule
import net.thunderbird.app.common.feature.appCommonFeatureModule
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import net.thunderbird.feature.mail.message.list.extension.toSortType
import org.koin.core.module.Module
import org.koin.dsl.module
import net.thunderbird.feature.mail.message.list.domain.DomainContract as MessageListDomainContract

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

    single<MessageListDomainContract.UseCase.GetDefaultSortType> {
        MessageListDomainContract.UseCase.GetDefaultSortType {
            K9.sortType.toSortType(isAscending = K9.isSortAscending(K9.sortType))
        }
    }
}
