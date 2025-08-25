package app.k9mail.feature.account.common

import app.k9mail.feature.account.common.data.InMemoryAccountStateRepository
import app.k9mail.feature.account.common.domain.AccountDomainContract
import com.fsck.k9.mail.oauth.AuthStateStorage
import net.thunderbird.core.common.coreCommonModule
import org.koin.core.module.Module
import org.koin.dsl.binds
import org.koin.dsl.module

val featureAccountCommonModule: Module = module {
    includes(coreCommonModule)

    single {
        InMemoryAccountStateRepository()
    }.binds(arrayOf(AccountDomainContract.AccountStateRepository::class, AuthStateStorage::class))
}
