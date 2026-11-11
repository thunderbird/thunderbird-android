package net.thunderbird.feature.account.avatar

import kotlin.time.ExperimentalTime
import net.thunderbird.feature.account.avatar.AvatarImageRepository
import net.thunderbird.feature.account.avatar.data.AvatarDataContract.DataSource
import net.thunderbird.feature.account.avatar.data.DefaultAvatarImageRepository
import net.thunderbird.feature.account.avatar.data.datasource.LocalAvatarImageDataSource
import org.koin.dsl.module

@OptIn(ExperimentalTime::class)
val featureAccountAvatarDiModule = module {
    single<DataSource.LocalAvatarImage> {
        LocalAvatarImageDataSource(
            fileManager = get(),
            directoryProvider = get(),
            mimeTypeResolver = get(),
            clock = get(), // fetch the app's global 'Clock'
        )
    }

    single<AvatarImageRepository> {
        DefaultAvatarImageRepository(
            localDataSource = get(),
        )
    }
}
