package net.thunderbird.feature.account.settings.impl.domain.entity

import net.thunderbird.feature.account.AccountId

internal enum class GeneralPreference {
    PROFILE,
    NAME,
    COLOR,
}

internal fun GeneralPreference.generateId(accountId: AccountId): String {
    return "${accountId.asRaw()}-general-${this.name.lowercase()}"
}
