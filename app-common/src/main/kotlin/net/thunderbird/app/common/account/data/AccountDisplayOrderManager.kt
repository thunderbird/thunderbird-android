package net.thunderbird.app.common.account.data

import net.thunderbird.feature.account.AccountId

/**
 * Manages the display order of accounts.
 */
interface AccountDisplayOrderManager {
    /**
     * Moves the account with the given [accountId] to the given [newPosition].
     *
     * @param accountId The ID of the account to move.
     * @param newPosition The new position of the account.
     */
    fun moveToPosition(accountId: AccountId, newPosition: Int)
}
