package net.thunderbird.feature.account.settings.impl.domain.entity

import net.thunderbird.feature.account.AccountId

/**
 * General preferences that can be set for an account.
 */
internal enum class GeneralPreference {
    PROFILE,
    PROFILE_INDICATOR,
    NAME,
    COLOR,
}

internal fun GeneralPreference.generateId(accountId: AccountId): String {
    return "${accountId.asRaw()}-general-${this.name.lowercase()}"
}
