package net.thunderbird.feature.account.storage.legacy

import net.thunderbird.feature.account.storage.legacy.mapper.DefaultAccountAvatarDataMapper
import net.thunderbird.feature.account.storage.legacy.mapper.DefaultAccountProfileDataMapper
import net.thunderbird.feature.account.storage.legacy.mapper.DefaultLegacyAccountDataMapper
import net.thunderbird.feature.account.storage.legacy.mapper.LegacyAccountDataMapper
import net.thunderbird.feature.account.storage.legacy.serializer.ServerSettingsDtoSerializer
import net.thunderbird.feature.account.storage.mapper.AccountAvatarDataMapper
import net.thunderbird.feature.account.storage.mapper.AccountProfileDataMapper
import org.koin.dsl.module

val featureAccountStorageLegacyModule = module {
    factory<LegacyAccountDataMapper> {
        DefaultLegacyAccountDataMapper()
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

    factory<AvatarDtoStorageHandler> {
        LegacyAvatarDtoStorageHandler()
    }

    factory<ProfileDtoStorageHandler> {
        LegacyProfileDtoStorageHandler(
            avatarDtoStorageHandler = get(),
        )
    }

    single<AccountDtoStorageHandler> {
        LegacyAccountStorageHandler(
            serverSettingsDtoSerializer = get(),
            profileDtoStorageHandler = get(),
            logger = get(),
        )
    }
}
