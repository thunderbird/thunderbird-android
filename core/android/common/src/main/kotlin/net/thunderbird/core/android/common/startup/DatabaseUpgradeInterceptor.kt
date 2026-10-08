package net.thunderbird.core.android.common.startup

import android.content.Context
import android.content.Intent

/**
 * Intercepts app startup to check if database upgrade is required.
 *
 * If databases need upgrading, this will launch the upgrade activity
 * and return true, indicating the calling activity should finish.
 */
interface DatabaseUpgradeInterceptor {
    /**
     * Checks if database upgrade is needed and launches upgrade activity if required.
     *
     * @param context The context used to start the upgrade activity
     * @param intent The intent that was used to start the calling activity
     * @return true if upgrade activity was launched (caller should finish), false otherwise
     */
    fun checkAndHandleUpgrade(context: Context, intent: Intent): Boolean
}
