package net.thunderbird.feature.account.core

import net.thunderbird.feature.account.core.data.DefaultAccountProfileRepository
import net.thunderbird.feature.account.profile.AccountProfileRepository
import org.koin.core.module.Module
import org.koin.dsl.module

val featureAccountCoreModule: Module = module {
    single<AccountProfileRepository> { DefaultAccountProfileRepository(get()) }
}
