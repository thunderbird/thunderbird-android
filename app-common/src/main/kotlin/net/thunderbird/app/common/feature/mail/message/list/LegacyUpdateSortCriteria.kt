package net.thunderbird.app.common.feature.mail.message.list

import com.fsck.k9.K9
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.message.list.domain.DomainContract.UseCase.UpdateSortCriteria
import net.thunderbird.feature.mail.message.list.domain.UpdateSortCriteriaOutcome
import net.thunderbird.feature.mail.message.list.domain.model.SortCriteria
import net.thunderbird.feature.mail.message.list.extension.toDomainSortType

private const val TAG = "LegacyUpdateSortCriteria"

class LegacyUpdateSortCriteria(
    private val logger: Logger,
    private val accountManager: LegacyAccountManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : UpdateSortCriteria {
    override suspend fun invoke(
        accountId: AccountId?,
        sortCriteria: SortCriteria,
    ): Outcome<UpdateSortCriteriaOutcome.Success, UpdateSortCriteriaOutcome.Error> =
        if (accountId == null) {
            updateUnifiedAccountSortCriteria(sortCriteria)
        } else {
            updateAccountSortCriteria(accountId, sortCriteria)
        }

    private suspend fun updateAccountSortCriteria(
        accountId: AccountId,
        sortCriteria: SortCriteria,
    ): Outcome<UpdateSortCriteriaOutcome.Success, UpdateSortCriteriaOutcome.Error> {
        logger.verbose(TAG) {
            "updateAccountSortCriteria() called with: accountId = $accountId, sortCriteria = $sortCriteria"
        }
        val (primary, secondary) = sortCriteria
        val (primarySortType, primarySortAscending) = primary.toDomainSortType()
        val secondaryCriteria = secondary?.toDomainSortType()
        logger.verbose(TAG) { "updateAccountSortCriteria: Setting primary sort criteria to $primarySortType" }
        secondaryCriteria?.let { (secondarySortType, _) ->
            logger.verbose(TAG) { "updateAccountSortCriteria: Setting secondary sort criteria to $secondarySortType" }
        }
        logger.debug(TAG) { "updateAccountSortCriteria: saving account with id $accountId" }
        val updatedAccount = accountManager.update(accountId) { account ->
            val accountSortAscending = account.sortAscending.toMutableMap()
            accountSortAscending[primarySortType] = primarySortAscending
            secondaryCriteria?.let { (secondarySortType, secondarySortAscending) ->
                accountSortAscending[secondarySortType] = secondarySortAscending
            }
            account.copy(
                sortType = primarySortType,
                sortAscending = accountSortAscending,
            )
        }
        if (updatedAccount == null) {
            logger.error(TAG) { "updateAccountSortCriteria: Could not find any account with id $accountId" }
            return Outcome.failure(UpdateSortCriteriaOutcome.Error.AccountNotFound(accountId))
        }

        return Outcome.success(UpdateSortCriteriaOutcome.Success)
    }

    private suspend fun updateUnifiedAccountSortCriteria(
        sortCriteria: SortCriteria,
    ): Outcome<UpdateSortCriteriaOutcome.Success, UpdateSortCriteriaOutcome.Error> {
        val (primary, secondary) = sortCriteria
        val (sortType, sortAscending) = primary.toDomainSortType()
        K9.sortType = sortType
        K9.setSortAscending(sortType, sortAscending)
        if (secondary != null) {
            val (secondarySortType, secondarySortAscending) = secondary.toDomainSortType()
            K9.setSortAscending(secondarySortType, secondarySortAscending)
        }
        withContext(ioDispatcher) {
            K9.saveSettingsAsync()
        }
        return Outcome.success(UpdateSortCriteriaOutcome.Success)
    }
}
