package net.thunderbird.feature.debug.settings.notification

import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import kotlin.reflect.KClass
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.notification.api.content.Notification

internal interface DebugNotificationSectionContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val accounts: ImmutableList<BaseAccount> = persistentListOf(),
        val selectedAccount: BaseAccount? = null,
        val notificationStatusLog: ImmutableList<String> = persistentListOf("Ready to send notification"),
        val selectedSystemNotificationType: KClass<out Notification>? = null,
        val selectedInAppNotificationType: KClass<out Notification>? = null,
        val folderName: String? = null,
        val singleNotificationData: MailSingleNotificationData = MailSingleNotificationData.Undefined,
        val systemNotificationTypes: ImmutableList<KClass<out Notification>> = persistentListOf(),
        val inAppNotificationTypes: ImmutableList<KClass<out Notification>> = persistentListOf(),
    ) {
        data class MailSingleNotificationData(
            val sender: String = "",
            val subject: String = "",
            val summary: String = "",
            val preview: String = "",
        ) {
            companion object {
                val Undefined = MailSingleNotificationData()
            }
        }
    }

    sealed interface Event {
        data class SelectAccount(val account: BaseAccount) : Event
        data class SelectNotificationType(val notificationType: KClass<out Notification>) : Event
        data object TriggerSystemNotification : Event
        data object TriggerInAppNotification : Event
        data class OnSenderChange(val sender: String) : Event
        data class OnSubjectChange(val subject: String) : Event
        data class OnSummaryChange(val summary: String) : Event
        data class OnPreviewChange(val preview: String) : Event
        data object ClearStatusLog : Event
    }

    sealed interface Effect
}
