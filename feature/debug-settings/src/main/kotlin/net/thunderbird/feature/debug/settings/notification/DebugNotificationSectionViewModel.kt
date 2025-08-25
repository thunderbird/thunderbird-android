package net.thunderbird.feature.debug.settings.notification

import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import kotlin.reflect.KClass
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.feature.debug.settings.R
import net.thunderbird.feature.debug.settings.notification.DebugNotificationSectionContract.Effect
import net.thunderbird.feature.debug.settings.notification.DebugNotificationSectionContract.Event
import net.thunderbird.feature.debug.settings.notification.DebugNotificationSectionContract.State
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
import net.thunderbird.feature.notification.api.receiver.InAppNotificationReceiver
import net.thunderbird.feature.notification.api.sender.NotificationSender

internal class DebugNotificationSectionViewModel(
    private val stringsResourceManager: StringsResourceManager,
    private val accountManager: AccountManager<BaseAccount>,
    private val notificationSender: NotificationSender,
    private val notificationReceiver: InAppNotificationReceiver,
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
                        add(MailNotification.NewMailSingleMail::class)
                        add(MailNotification.NewMailSummaryMail::class)
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

        viewModelScope.launch {
            notificationReceiver
                .events
                .collectLatest { event ->
                    updateState { state ->
                        state.copy(
                            notificationStatusLog = state.notificationStatusLog + " In-app notification event: $event",
                        )
                    }
                }
        }
    }

    override fun event(event: Event) {
        when (event) {
            is Event.TriggerSystemNotification -> viewModelScope.launch {
                if (state.value.selectedSystemNotificationType == null) {
                    updateState {
                        it.copy(selectedSystemNotificationType = state.value.systemNotificationTypes.first())
                    }
                }
                triggerNotification(
                    notification = requireNotNull(buildNotification(state.value.selectedSystemNotificationType)),
                )
            }

            is Event.TriggerInAppNotification -> viewModelScope.launch {
                if (state.value.selectedInAppNotificationType == null) {
                    updateState {
                        it.copy(selectedInAppNotificationType = state.value.inAppNotificationTypes.first())
                    }
                }
                triggerNotification(
                    notification = requireNotNull(buildNotification(state.value.selectedInAppNotificationType)),
                )
            }

            is Event.SelectAccount -> updateState { state ->
                state.copy(selectedAccount = event.account)
            }

            is Event.SelectNotificationType -> viewModelScope.launch {
                buildNotification(event.notificationType)
            }

            is Event.OnSenderChange -> updateState {
                it.copy(singleNotificationData = it.singleNotificationData.copy(sender = event.sender))
            }

            is Event.OnSubjectChange -> updateState {
                it.copy(singleNotificationData = it.singleNotificationData.copy(subject = event.subject))
            }

            is Event.OnSummaryChange -> updateState {
                it.copy(singleNotificationData = it.singleNotificationData.copy(summary = event.summary))
            }

            is Event.OnPreviewChange -> updateState {
                it.copy(singleNotificationData = it.singleNotificationData.copy(preview = event.preview))
            }

            Event.ClearStatusLog -> updateState { it.copy(notificationStatusLog = persistentListOf()) }
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
                    stringsResourceManager.stringResource(
                        R.string.debug_settings_notifications_preparing_notification,
                        notificationType?.realName,
                    ),
            )
        }

        val state = state.value
        val selectedAccount = state.selectedAccount ?: return null
        val accountDisplay = selectedAccount.name ?: selectedAccount.email

        val notification = buildNotification(
            notificationType = notificationType,
            selectedAccount = selectedAccount,
            accountDisplay = accountDisplay,
            state = state,
        )

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
        selectedAccount: BaseAccount,
        accountDisplay: String,
        state: State,
    ): Notification? = when (notificationType) {
        AuthenticationErrorNotification::class -> AuthenticationErrorNotification(
            isIncomingServerError = true,
            accountUuid = selectedAccount.uuid,
            accountDisplayName = accountDisplay,
            accountNumber = 0,
        )

        CertificateErrorNotification::class -> CertificateErrorNotification(
            isIncomingServerError = true,
            accountUuid = selectedAccount.uuid,
            accountDisplayName = accountDisplay,
            accountNumber = 0,
        )

        FailedToCreateNotification::class -> FailedToCreateNotification(
            accountUuid = selectedAccount.uuid,
            failedNotification = AuthenticationErrorNotification(
                isIncomingServerError = true,
                accountUuid = selectedAccount.uuid,
                accountDisplayName = accountDisplay,
                accountNumber = 0,
            ),
        )

        MailNotification.Fetching::class -> MailNotification.Fetching(
            accountUuid = selectedAccount.uuid,
            accountDisplayName = accountDisplay,
            folderName = state.folderName,
        )

        MailNotification.NewMailSingleMail::class -> state.buildSingleMailNotification(
            selectedAccount = selectedAccount,
            accountDisplay = accountDisplay,
        )

        MailNotification.NewMailSummaryMail::class -> MailNotification.NewMailSummaryMail(
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
            accountUuid = selectedAccount.uuid,
            exception = Exception("What a failure"),
        )

        MailNotification.Sending::class -> MailNotification.Sending(
            accountUuid = selectedAccount.uuid,
            accountDisplayName = accountDisplay,
        )

        PushServiceNotification.AlarmPermissionMissing::class -> PushServiceNotification.AlarmPermissionMissing()

        PushServiceNotification.Initializing::class -> PushServiceNotification.Initializing()

        PushServiceNotification.Listening::class -> PushServiceNotification.Listening()

        PushServiceNotification.WaitBackgroundSync::class -> PushServiceNotification.WaitBackgroundSync()

        PushServiceNotification.WaitNetwork::class -> PushServiceNotification.WaitNetwork()

        else -> null
    }

    private fun State.buildSingleMailNotification(
        selectedAccount: BaseAccount,
        accountDisplay: String,
    ): MailNotification.NewMailSingleMail? = MailNotification.NewMailSingleMail(
        accountUuid = selectedAccount.uuid,
        accountName = accountDisplay,
        messagesNotificationChannelSuffix = "",
        summary = singleNotificationData.summary,
        sender = singleNotificationData.sender,
        subject = singleNotificationData.subject,
        preview = singleNotificationData.preview,
        group = null,
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
