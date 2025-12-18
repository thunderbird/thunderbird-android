package net.thunderbird.feature.account

/**
 * Interface representing an account by its unique identifier [AccountId].
 *
 * @property id The unique identifier of the account.
 */
interface Account : AccountIdentifiable {
    override val id: AccountId
}
