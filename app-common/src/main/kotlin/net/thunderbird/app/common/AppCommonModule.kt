package net.thunderbird.app.common

import com.fsck.k9.K9
import com.fsck.k9.legacyCommonAppModules
import com.fsck.k9.legacyCoreModules
import com.fsck.k9.legacyUiModules
import net.thunderbird.app.common.account.appCommonAccountModule
import net.thunderbird.app.common.appConfig.AndroidPlatformConfigProvider
import net.thunderbird.app.common.core.appCommonCoreModule
import net.thunderbird.app.common.feature.appCommonFeatureModule
import net.thunderbird.app.common.feature.mail.message.list.LegacyUpdateSortCriteria
import net.thunderbird.app.common.startup.appCommonStartupModule
import net.thunderbird.core.android.account.SortType
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import net.thunderbird.feature.mail.message.list.domain.model.SortCriteria
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
        appCommonStartupModule,
    )

    single<PlatformConfigProvider> { AndroidPlatformConfigProvider() }

    single<MessageListDomainContract.UseCase.GetDefaultSortCriteria> {
        MessageListDomainContract.UseCase.GetDefaultSortCriteria {
            val primary = K9.sortType.toSortType(isAscending = K9.isSortAscending(K9.sortType))
            val secondary = K9.sortType
                .takeIf { it != SortType.SORT_DATE }
                ?.let(K9::isSortAscending)
                ?.let(SortType.SORT_DATE::toSortType)
            SortCriteria(primary = primary, secondary = secondary)
        }
    }
    single<MessageListDomainContract.UseCase.UpdateSortCriteria> {
        LegacyUpdateSortCriteria(logger = get(), accountManager = get())
    }
}
