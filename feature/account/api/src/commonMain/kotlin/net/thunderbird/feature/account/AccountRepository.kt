package net.thunderbird.feature.account

interface AccountRepository {

    /**
     * Deletes the specified account.
     *
     * @param accountId The account to delete.
     */
    fun delete(accountId: AccountId)
}
