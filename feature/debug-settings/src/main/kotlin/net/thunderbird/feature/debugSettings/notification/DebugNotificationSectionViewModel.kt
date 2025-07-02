package net.thunderbird.feature.debugSettings.notification

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import kotlin.reflect.KClass
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thunderbird.feature.debugSettings.notification.DebugNotificationSectionContract.Effect
import net.thunderbird.feature.debugSettings.notification.DebugNotificationSectionContract.Event
import net.thunderbird.feature.debugSettings.notification.DebugNotificationSectionContract.State
import net.thunderbird.feature.mail.account.api.AccountManager
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.notification.api.NotificationGroup
import net.thunderbird.feature.notification.api.NotificationGroupKey
import net.thunderbird.feature.notification.api.content.AuthenticationErrorNotification
import net.thunderbird.feature.notification.api.content.CertificateErrorNotification
import net.thunderbird.feature.notification.api.content.FailedToCreateNotification
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.content.MailNotification
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.content.PushServiceNotification
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.sender.NotificationSender

internal class DebugNotificationSectionViewModel(
    private val accountManager: AccountManager<BaseAccount>,
    private val notificationSender: NotificationSender,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel<State, Event, Effect>(initialState = State()), DebugNotificationSectionContract.ViewModel {

    init {
        viewModelScope.launch(ioDispatcher) {
            val accounts = accountManager.getAccounts()
            withContext(mainDispatcher) {
                updateState {
                    val systemNotificationTypes = buildList {
                        add(AuthenticationErrorNotification::class)
                        add(CertificateErrorNotification::class)
                        add(FailedToCreateNotification::class)
                        add(MailNotification.Fetching::class)
                        add(MailNotification.NewMail.SingleMail::class)
                        add(MailNotification.NewMail.SummaryMail::class)
                        add(MailNotification.SendFailed::class)
                        add(MailNotification.Sending::class)
                        add(PushServiceNotification.AlarmPermissionMissing::class)
                        add(PushServiceNotification.Initializing::class)
                        add(PushServiceNotification.Listening::class)
                        add(PushServiceNotification.WaitBackgroundSync::class)
                        add(PushServiceNotification.WaitNetwork::class)
                    }.toPersistentList()

                    val inAppNotificationTypes = buildList {
                        add(AuthenticationErrorNotification::class)
                        add(CertificateErrorNotification::class)
                        add(FailedToCreateNotification::class)
                        add(MailNotification.SendFailed::class)
                        add(PushServiceNotification.AlarmPermissionMissing::class)
                    }.toPersistentList()
                    State(
                        accounts = accounts.toPersistentList(),
                        selectedAccount = accounts.first(),
                        systemNotificationTypes = systemNotificationTypes,
                        inAppNotificationTypes = inAppNotificationTypes,
                        selectedSystemNotificationType = systemNotificationTypes.first(),
                        selectedInAppNotificationType = inAppNotificationTypes.first(),
                    )
                }
            }
        }
    }

    override fun event(event: DebugNotificationSectionContract.Event) {
        when (event) {
            is DebugNotificationSectionContract.Event.TriggerSystemNotification -> viewModelScope.launch {
                if (state.value.selectedSystemNotificationType == null) {
                    updateState {
                        it.copy(selectedSystemNotificationType = state.value.systemNotificationTypes.first())
                    }
                }
                triggerNotification(
                    notification = requireNotNull(buildNotification(state.value.selectedSystemNotificationType)),
                )
            }

            is DebugNotificationSectionContract.Event.TriggerInAppNotification -> viewModelScope.launch {
                if (state.value.selectedInAppNotificationType == null) {
                    updateState {
                        it.copy(selectedInAppNotificationType = state.value.inAppNotificationTypes.first())
                    }
                }
                triggerNotification(
                    notification = requireNotNull(buildNotification(state.value.selectedInAppNotificationType)),
                )
            }

            is DebugNotificationSectionContract.Event.SelectAccount -> updateState { state ->
                state.copy(selectedAccount = event.account)
            }

            is DebugNotificationSectionContract.Event.SelectNotificationType -> viewModelScope.launch {
                buildNotification(event.notificationType)
            }

            is DebugNotificationSectionContract.Event.OnSenderChange -> updateState {
                it.copy(singleNotificationData = it.singleNotificationData.copy(sender = event.sender))
            }

            is DebugNotificationSectionContract.Event.OnSubjectChange -> updateState {
                it.copy(singleNotificationData = it.singleNotificationData.copy(subject = event.subject))
            }

            is DebugNotificationSectionContract.Event.OnSummaryChange -> updateState {
                it.copy(singleNotificationData = it.singleNotificationData.copy(summary = event.summary))
            }

            is DebugNotificationSectionContract.Event.OnPreviewChange -> updateState {
                it.copy(singleNotificationData = it.singleNotificationData.copy(preview = event.preview))
            }
        }
    }

    private suspend fun triggerNotification(
        notification: Notification,
    ) {
        notification.let { notification ->
            notificationSender
                .send(notification)
                .collect { result ->
                    updateState {
                        it.copy(notificationStatusLog = it.notificationStatusLog + "Result: $result")
                    }
                }
        }
    }

    private suspend fun buildNotification(notificationType: KClass<out Notification>?): Notification? {
        updateState {
            it.copy(
                notificationStatusLog = it.notificationStatusLog +
                    "Preparing notification ${notificationType?.realName}",
            )
        }

        val state = state.value
        val selectedAccount = state.selectedAccount ?: return null
        val accountDisplay = selectedAccount.name ?: selectedAccount.email
        val accountNumber = 1 // TODO: retrieve accountNumber from?

        val notification = buildNotification(notificationType, accountNumber, selectedAccount, accountDisplay, state)

        updateState { state ->
            state.copy(
                selectedSystemNotificationType = (notification as? SystemNotification)?.let { it::class }
                    ?: state.selectedSystemNotificationType,
                selectedInAppNotificationType = (notification as? InAppNotification)?.let { it::class }
                    ?: state.selectedInAppNotificationType,
            )
        }

        return notification
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private suspend fun buildNotification(
        notificationType: KClass<out Notification>?,
        accountNumber: Int,
        selectedAccount: BaseAccount,
        accountDisplay: String,
        state: State,
    ): Notification? = when (notificationType) {
        AuthenticationErrorNotification::class -> AuthenticationErrorNotification(
            accountNumber = accountNumber,
            accountUuid = selectedAccount.uuid,
            accountDisplayName = accountDisplay,
        )

        CertificateErrorNotification::class -> CertificateErrorNotification(
            accountNumber = accountNumber,
            accountUuid = selectedAccount.uuid,
            accountDisplayName = accountDisplay,
        )

        FailedToCreateNotification::class -> FailedToCreateNotification(
            accountNumber = accountNumber,
            accountUuid = selectedAccount.uuid,
            failedNotification = AuthenticationErrorNotification(
                accountNumber = accountNumber,
                accountUuid = selectedAccount.uuid,
                accountDisplayName = accountDisplay,
            ),
        )

        MailNotification.Fetching::class -> MailNotification.Fetching(
            accountNumber = accountNumber,
            accountUuid = selectedAccount.uuid,
            accountDisplayName = accountDisplay,
            folderName = state.folderName,
        )

        MailNotification.NewMail.SingleMail::class -> state.buildSingleMailNotification(
            accountNumber = accountNumber,
            selectedAccount = selectedAccount,
            accountDisplay = accountDisplay,
        )

        MailNotification.NewMail.SummaryMail::class -> MailNotification.NewMail.SummaryMail(
            accountNumber = accountNumber,
            accountUuid = selectedAccount.uuid,
            accountDisplayName = accountDisplay,
            messagesNotificationChannelSuffix = "",
            newMessageCount = 10,
            additionalMessagesCount = 10,
            group = NotificationGroup(
                key = NotificationGroupKey("key"),
                summary = "",
            ),
        )

        MailNotification.SendFailed::class -> MailNotification.SendFailed(
            accountNumber = accountNumber,
            accountUuid = selectedAccount.uuid,
            exception = Exception("What a failure"),
        )

        MailNotification.Sending::class -> MailNotification.Sending(
            accountNumber = accountNumber,
            accountUuid = selectedAccount.uuid,
            accountDisplayName = accountDisplay,
        )

        PushServiceNotification.AlarmPermissionMissing::class -> PushServiceNotification.AlarmPermissionMissing(
            accountNumber = accountNumber,
        )

        PushServiceNotification.Initializing::class -> PushServiceNotification.Initializing(
            accountNumber = accountNumber,
        )

        PushServiceNotification.Listening::class -> PushServiceNotification.Listening(
            accountNumber = accountNumber,
        )

        PushServiceNotification.WaitBackgroundSync::class -> PushServiceNotification.WaitBackgroundSync(
            accountNumber = accountNumber,
        )

        PushServiceNotification.WaitNetwork::class -> PushServiceNotification.WaitNetwork(
            accountNumber = accountNumber,
        )

        else -> null
    }

    private fun State.buildSingleMailNotification(
        accountNumber: Int,
        selectedAccount: BaseAccount,
        accountDisplay: String,
    ): MailNotification.NewMail.SingleMail? = MailNotification.NewMail.SingleMail(
        accountNumber = accountNumber,
        accountUuid = selectedAccount.uuid,
        accountName = accountDisplay,
        messagesNotificationChannelSuffix = "",
        summary = singleNotificationData.summary,
        sender = singleNotificationData.sender,
        subject = singleNotificationData.subject,
        preview = singleNotificationData.preview,
    )

    private operator fun ImmutableList<String>.plus(other: String): ImmutableList<String> =
        (this.toMutableList() + other).toPersistentList()
}

internal val KClass<out Notification>.realName: String
    get() {
        val clazz = java

        return clazz.name
            .replace(clazz.`package`?.name.orEmpty(), "")
            .removePrefix(".")
            .replace("$", ".")
    }
