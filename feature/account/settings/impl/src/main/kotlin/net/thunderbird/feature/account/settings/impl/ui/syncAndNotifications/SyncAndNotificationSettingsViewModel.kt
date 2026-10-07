package net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications

import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.impl.ui.readingMail.ReadingMailSettingsContract
import net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications.SyncAndNotificationSettingsContract.Effect
import net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications.SyncAndNotificationSettingsContract.Event
import net.thunderbird.feature.account.settings.impl.ui.syncAndNotifications.SyncAndNotificationSettingsContract.State

internal class SyncAndNotificationSettingsViewModel(
    private val accountId: AccountId,
    initialState: State = State(
        enablePushNotifications = true,
    )
) : BaseViewModel<State, Event, Effect>(initialState), SyncAndNotificationSettingsContract.ViewModel {
    override fun event(event: Event) {
        when (event) {
            is Event.OnBackPressed -> emitEffect(Effect.NavigateBack)
            is Event.OnEnablePushNotificationChange -> TODO()
            Event.OnSelectFetchingAndDataLimits -> TODO()
            Event.OnSelectFolderSyncAndPush -> TODO()
            Event.OnSelectNotificationStyle -> TODO()
        }
    }
}
