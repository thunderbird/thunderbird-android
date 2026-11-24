package net.thunderbird.feature.account.settings.api

import net.thunderbird.feature.account.AccountId

interface BackgroundAccountRemover {
    fun removeAccountAsync(accountId: AccountId)
}
