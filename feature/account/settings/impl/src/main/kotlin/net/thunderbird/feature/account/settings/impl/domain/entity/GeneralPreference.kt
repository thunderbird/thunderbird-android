package net.thunderbird.feature.account.settings.impl.domain.entity

import net.thunderbird.feature.account.api.AccountId

internal enum class GeneralPreference {
    PROFILE,
    NAME,
    COLOR,
}

internal fun GeneralPreference.generateId(accountId: AccountId): String {
    return "${accountId.value}-general-${this.name.lowercase()}"
}
