package com.fsck.k9.ui.settings

import android.app.Activity
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.ui.settings.general.GeneralSettingsActivity

internal enum class SettingsAction {
    GENERAL_SETTINGS {
        override fun execute(activity: Activity) {
            GeneralSettingsActivity.start(activity)
        }
    },
    ADD_ACCOUNT {
        override fun execute(activity: Activity) {
            AccountSetupBasics.actionNewAccount(activity)
        }
    },
    ABOUT_SCREEN {
        override fun execute(activity: Activity) {
            AboutActivity.start(activity)
        }
    };

    abstract fun execute(activity: Activity)
}
