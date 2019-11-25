package com.fsck.k9.ui.settings.encryption


class SettingsEncryptionUiModel {
    var settingsList: List<SettingsListItem> = emptyList()
    var isSettingsListEnabled = true

    fun initializeSettingsList(list: List<SettingsListItem>) {
        settingsList = list
    }

    fun setSettingsListItemSelection(position: Int, encryptionEnabled: Boolean) {
        settingsList[position].enabled = encryptionEnabled
    }

}

sealed class SettingsListItem {
    var enabled: Boolean = true

    object AdvancedSettings : SettingsListItem()
    data class EncryptionIdentity(
            val id: Int,
            val accountNumber: Int,
            val email: String
    ) : SettingsListItem()
}
