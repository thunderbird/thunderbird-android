package net.thunderbird.feature.account.settings.api

import android.content.Context
import net.thunderbird.feature.account.AccountId

/** Legacy activity launches provided by the host application. */
interface AccountSettingsExternalContract {
    /**
     * Launches legacy settings activities in the caller's task.
     *
     * Each context must be an Activity or an activity-backed ContextWrapper. Application contexts are unsupported:
     * these launches do not add FLAG_ACTIVITY_NEW_TASK, preserving the caller's task and Back behavior.
     */
    interface Launcher {
        fun launchFolderSettings(context: Context, accountId: AccountId)
        fun launchNotificationStyleSettings(context: Context, accountId: AccountId)
        fun launchFolderSyncAndPushSettings(context: Context, accountId: AccountId)
    }
}
