package com.fsck.k9.ui.settings

import com.fsck.k9.ui.R
import com.xwray.groupie.Section

@Deprecated("Remove this once we switch over to the new setup UI")
object NewSetupUiHack {
    fun addSettingsActionItem(section: Section) {
        val addAccountActionItem = SettingsActionItem(
            "Add account (NEW)",
            R.id.action_settingsListScreen_to_newAddAccountScreen,
            R.attr.iconSettingsAccountAdd
        )
        section.add(addAccountActionItem)
    }
}
