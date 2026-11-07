package net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications

import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications.SyncAndNotificationSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications.SyncAndNotificationSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications.SyncAndNotificationSettingsContract.State

internal class SyncAndNotificationSettingsViewModel(
    private val accountId: AccountId, // TODO #11683, will be necessary for feature
) : SyncAndNotificationSettingsContract.ViewModel() {
    override fun event(event: Event) {
        when (event) {
            is Event.OnBackPressed -> emitEffect(Effect.NavigateBack)

            is Event.OnEnablePushNotificationChange -> { /* TODO #11683 */ }

            Event.OnSelectNotificationStyle -> {
                emitEffect(Effect.NavigateToNotificationStyle)
            }

            Event.OnSelectFolderSyncAndPush -> {
                emitEffect(Effect.NavigateToFolderSyncAndPush)
            }

            Event.OnSelectFetchingAndDataLimits -> {
                emitEffect(Effect.NavigateToFetchingAndDataLimits)
            }
        }
    }
}
