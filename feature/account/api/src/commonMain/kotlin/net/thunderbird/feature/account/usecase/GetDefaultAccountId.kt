package net.thunderbird.feature.account.usecase

import net.thunderbird.feature.account.AccountId

/**
 * Returns the default account id.
 */
fun interface GetDefaultAccountId {
    operator fun invoke(): AccountId?
}
