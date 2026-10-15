package net.thunderbird.app.common.feature.account.usecase

import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.usecase.GetDefaultAccountId

class DefaultGetDefaultAccountId(
    private val accountManager: LegacyAccountManager,
) : GetDefaultAccountId {
    override operator fun invoke(): AccountId? {
        return accountManager.findAll().firstOrNull()?.id
    }
}
