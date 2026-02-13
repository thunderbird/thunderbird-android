package net.thunderbird.feature.applock.api

import android.content.Context
import android.content.Intent

fun interface AppLockSettingsNavigation {
    fun createIntent(context: Context): Intent
}
