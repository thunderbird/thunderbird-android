package net.thunderbird.feature.account.storage.legacy

import net.thunderbird.feature.account.AccountId

/**
 * Generates keys for account storage.
 */
class AccountKeyGenerator(
    private val id: AccountId,
) {

    /**
     * Creates a key by combining account ID with the specified key.
     *
     * @param key The key to combine with the account ID.
     * @throws IllegalArgumentException if the key is empty.
     */
    fun create(key: String): String {
        require(key.isNotEmpty()) { "Key must not be empty" }
        return "$id.$key"
    }
}
