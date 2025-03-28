package net.thunderbird.feature.account.core

import net.thunderbird.feature.account.api.profile.AccountProfileRepository
import net.thunderbird.feature.account.core.data.DefaultAccountProfileRepository
import org.koin.core.module.Module
import org.koin.dsl.module

val featureAccountCoreModule: Module = module {
    single<AccountProfileRepository> { DefaultAccountProfileRepository(get()) }
}
