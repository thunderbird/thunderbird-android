package net.thunderbird.feature.mail.message.list.internal.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.extension.toSortType
import net.thunderbird.feature.mail.message.list.ui.state.SortType

class GetSortTypes(
    private val accountManager: LegacyAccountManager,
    private val getDefaultSortType: DomainContract.UseCase.GetDefaultSortType,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DomainContract.UseCase.GetSortTypes {
    override suspend operator fun invoke(accountIds: Set<AccountId>): Map<AccountId?, SortType> {
        val accounts = withContext(ioDispatcher) {
            accountManager.getAccounts()
        }
        val sortTypes = buildMap {
            put(null, getDefaultSortType())
            putAll(
                accounts
                    .associate {
                        it.id to it.sortType.toSortType(isAscending = it.sortAscending[it.sortType] ?: false)
                    },
            )
        }
        return sortTypes
    }
}
