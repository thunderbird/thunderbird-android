package net.thunderbird.feature.account.storage.legacy

import net.thunderbird.feature.account.storage.legacy.mapper.DefaultAccountAvatarDataMapper
import net.thunderbird.feature.account.storage.legacy.mapper.DefaultAccountProfileDataMapper
import net.thunderbird.feature.account.storage.legacy.mapper.DefaultLegacyAccountWrapperDataMapper
import net.thunderbird.feature.account.storage.legacy.serializer.ServerSettingsDtoSerializer
import net.thunderbird.feature.account.storage.mapper.AccountAvatarDataMapper
import net.thunderbird.feature.account.storage.mapper.AccountProfileDataMapper
import org.koin.dsl.module

val featureAccountStorageLegacyModule = module {
    factory {
        DefaultLegacyAccountWrapperDataMapper()
    }

    factory<AccountAvatarDataMapper> {
        DefaultAccountAvatarDataMapper()
    }

    factory<AccountProfileDataMapper> {
        DefaultAccountProfileDataMapper(
            avatarMapper = get(),
        )
    }

    factory { ServerSettingsDtoSerializer() }

    single {
        LegacyAccountStorageHandler(
            serverSettingsDtoSerializer = get(),
            logger = get(),
        )
    }
}
