package net.thunderbird.feature.account.usecase

import net.thunderbird.feature.account.AccountId

/**
 * Deletes the specified account.
 */
fun interface DeleteAccount {
    suspend operator fun invoke(accountId: AccountId)
}
