package net.thunderbird.app.common.feature.account

import android.content.Context
import com.fsck.k9.ui.managefolders.ManageFoldersActivity
import com.fsck.k9.ui.settings.account.AccountSettingsActivity
import com.fsck.k9.ui.settings.account.AccountSettingsFragment
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.api.AccountSettingsExternalContract

internal class DefaultAccountSettingsLauncher : AccountSettingsExternalContract.Launcher {
    override fun launchFolderSettings(context: Context, accountId: AccountId) {
        AccountSettingsActivity.start(
            context = context,
            accountUuid = accountId.value.toString(),
            startScreenKey = AccountSettingsFragment.PREFERENCE_FOLDERS,
        )
    }

    override fun launchNotificationStyleSettings(context: Context, accountId: AccountId) {
        AccountSettingsActivity.start(
            context = context,
            accountUuid = accountId.value.toString(),
            startScreenKey = AccountSettingsFragment.PREFERENCE_NOTIFICATION_STYLE,
        )
    }

    override fun launchFolderSyncAndPushSettings(context: Context, accountId: AccountId) {
        ManageFoldersActivity.launch(context, accountId)
    }
}
