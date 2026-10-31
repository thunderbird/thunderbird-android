package com.fsck.k9.ui.settings.export

sealed interface SettingsListItem {

    val stableId: String
    val selected: Boolean

    data class GeneralSettings(
        override val selected: Boolean = true,
    ) : SettingsListItem {
        override val stableId = "general"
    }

    data class Account(
        val accountNumber: Int,
        val displayName: String,
        val email: String,
        override val selected: Boolean = true,
    ) : SettingsListItem {
        override val stableId = accountNumber.toString()
    }
}
