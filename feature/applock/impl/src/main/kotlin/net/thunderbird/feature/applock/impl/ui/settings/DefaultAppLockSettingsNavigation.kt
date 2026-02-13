package net.thunderbird.feature.applock.impl.ui.settings

import android.content.Context
import android.content.Intent
import net.thunderbird.feature.applock.api.AppLockSettingsNavigation

internal class DefaultAppLockSettingsNavigation : AppLockSettingsNavigation {
    override fun createIntent(context: Context): Intent {
        return AppLockSettingsActivity.createIntent(context)
    }
}
