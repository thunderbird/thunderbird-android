package net.thunderbird.feature.account.avatar.di

import net.thunderbird.feature.account.avatar.AvatarImageRepository
import net.thunderbird.feature.account.avatar.data.AvatarDataContract.DataSource
import net.thunderbird.feature.account.avatar.data.DefaultAvatarImageRepository
import net.thunderbird.feature.account.avatar.data.datasource.LocalAvatarImageDataSource
import org.koin.dsl.module

val featureAccountAvatarModule = module {
    single<DataSource.LocalAvatarImage> {
        LocalAvatarImageDataSource(
            fileManager = get(),
            directoryProvider = get(),
        )
    }

    single<AvatarImageRepository> {
        DefaultAvatarImageRepository(
            localDataSource = get(),
        )
    }
}
