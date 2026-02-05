package net.thunderbird.feature.mail.message.list.internal.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.domain.model.SortCriteria
import net.thunderbird.feature.mail.message.list.domain.model.SortType
import net.thunderbird.feature.mail.message.list.extension.toSortType
import net.thunderbird.core.android.account.SortType as LegacySortType

class GetSortCriteriaPerAccount(
    private val accountManager: LegacyAccountManager,
    private val getDefaultSortCriteria: DomainContract.UseCase.GetDefaultSortCriteria,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DomainContract.UseCase.GetSortCriteriaPerAccount {

    private val LegacyAccount.primarySortType: SortType
        get() = sortType.toSortType(isAscending = sortAscending[sortType] ?: false)

    private val LegacyAccount.secondarySortType: SortType?
        get() = sortAscending[LegacySortType.SORT_DATE]
            ?.takeIf { primarySortType !in SortCriteria.SecondaryNotRequiredForSortTypes }
            ?.let(LegacySortType.SORT_DATE::toSortType)

    override suspend operator fun invoke(accountIds: Set<AccountId>): Map<AccountId?, SortCriteria> {
        val accounts = withContext(ioDispatcher) { accountManager.getAccounts() }
        val sortCriteria = buildMap {
            put(null, getDefaultSortCriteria())
            putAll(
                accounts.associate { account ->
                    account.id to SortCriteria(
                        primary = account.primarySortType,
                        secondary = account.secondarySortType,
                    )
                },
            )
        }
        return sortCriteria
    }
}
