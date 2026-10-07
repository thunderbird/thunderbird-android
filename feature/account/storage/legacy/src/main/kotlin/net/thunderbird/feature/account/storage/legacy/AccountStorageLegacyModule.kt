package net.thunderbird.feature.account.storage.legacy

import net.thunderbird.feature.account.storage.legacy.mapper.DefaultAccountProfileDataMapper
import net.thunderbird.feature.account.storage.legacy.mapper.DefaultAvatarDataMapper
import net.thunderbird.feature.account.storage.legacy.serializer.ServerSettingsDtoSerializer
import net.thunderbird.feature.account.storage.mapper.AccountProfileDataMapper
import net.thunderbird.feature.account.storage.mapper.AvatarDataMapper
import org.koin.dsl.module

val featureAccountStorageLegacyModule = module {
    factory<AvatarDataMapper> {
        DefaultAvatarDataMapper()
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

    single<AccountStorageHandler> {
        LegacyAccountStorageHandler(
            serverSettingsDtoSerializer = get(),
            profileDtoStorageHandler = get(),
            generalSettingsManager = get(),
            logger = get(),
        )
    }
}
