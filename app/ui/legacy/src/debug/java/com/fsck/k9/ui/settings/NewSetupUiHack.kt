package com.fsck.k9.ui.settings

import com.fsck.k9.ui.R
import com.fsck.k9.ui.settings.SettingsListFragment.SettingsListBuilder

@Deprecated("Remove this once we switch over to the new setup UI")
internal object NewSetupUiHack {
    fun addAction(builder: SettingsListBuilder) {
        builder.addAction(
            "Add account (NEW)",
            R.id.action_settingsListScreen_to_newAddAccountScreen,
            R.attr.iconSettingsAccountAdd
        )
    }
}
