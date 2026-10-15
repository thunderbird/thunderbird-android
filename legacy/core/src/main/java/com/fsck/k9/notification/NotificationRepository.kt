package com.fsck.k9.notification

import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.mailstore.LocalStoreProvider
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.LockScreenNotificationVisibility
import net.thunderbird.feature.account.AccountId

internal class NotificationRepository(
    private val notificationStoreProvider: NotificationStoreProvider,
    private val localStoreProvider: LocalStoreProvider,
    private val messageStoreManager: MessageStoreManager,
    private val notificationContentCreator: NotificationContentCreator,
    private val generalSettingsManager: GeneralSettingsManager,
    private val notificationDataStore: NotificationDataStore,
    private val accountManager: LegacyAccountManager,
) {
    private val lockScreenNotificationVisibility: LockScreenNotificationVisibility
        get() = generalSettingsManager.getConfig().notification.lockScreenNotificationVisibility

    @Synchronized
    fun restoreNotifications(accountId: AccountId): NotificationData? {
        if (notificationDataStore.isAccountInitialized(accountId)) return null

        return accountManager.findById(accountId)?.let { account ->
            val localStore = localStoreProvider.getInstance(accountId)

            val (activeNotificationMessages, inactiveNotificationMessages) = localStore.notificationMessages.partition {
                it.notificationId != null
            }

            val activeNotifications = activeNotificationMessages.map { notificationMessage ->
                val isFromSelf = account.isAnIdentity(notificationMessage.message.from)
                val content = notificationContentCreator.createFromMessage(notificationMessage.message, isFromSelf)
                NotificationHolder(notificationMessage.notificationId!!, notificationMessage.timestamp, content)
            }

            val inactiveNotifications = inactiveNotificationMessages.map { notificationMessage ->
                val isFromSelf = account.isAnIdentity(notificationMessage.message.from)
                val content = notificationContentCreator.createFromMessage(notificationMessage.message, isFromSelf)
                InactiveNotificationHolder(notificationMessage.timestamp, content)
            }

            notificationDataStore.initializeAccount(
                accountId,
                activeNotifications,
                inactiveNotifications,
                lockScreenNotificationVisibility,
            ).takeIf { it.activeNotifications.isNotEmpty() }
        }
    }

    @Synchronized
    fun addNotification(
        accountId: AccountId,
        content: NotificationContent,
        timestamp: Long,
    ): AddNotificationResult? {
        restoreNotifications(accountId)

        return notificationDataStore.addNotification(accountId, content, timestamp)?.also { result ->
            persistNotificationDataStoreChanges(
                accountId = accountId,
                operations = result.notificationStoreOperations,
                updateNewMessageState = true,
            )
        }
    }

    @Synchronized
    fun removeNotifications(
        accountId: AccountId,
        clearNewMessageState: Boolean = true,
        selector: (List<MessageReference>) -> List<MessageReference>,
    ): RemoveNotificationsResult? {
        restoreNotifications(accountId)

        return notificationDataStore.removeNotifications(accountId, selector)?.also { result ->
            persistNotificationDataStoreChanges(
                accountId = accountId,
                operations = result.notificationStoreOperations,
                updateNewMessageState = clearNewMessageState,
            )
        }
    }

    @Synchronized
    fun clearNotifications(accountId: AccountId, clearNewMessageState: Boolean) {
        notificationDataStore.clearNotifications(accountId)
        clearNotificationStore(accountId)

        if (clearNewMessageState) {
            clearNewMessageState(accountId)
        }
    }

    private fun persistNotificationDataStoreChanges(
        accountId: AccountId,
        operations: List<NotificationStoreOperation>,
        updateNewMessageState: Boolean,
    ) {
        val notificationStore = notificationStoreProvider.getNotificationStore(accountId)
        notificationStore.persistNotificationChanges(operations)

        if (updateNewMessageState) {
            setNewMessageState(accountId, operations)
        }
    }

    private fun setNewMessageState(accountId: AccountId, operations: List<NotificationStoreOperation>) {
        val messageStore = messageStoreManager.getMessageStore(accountId)

        for (operation in operations) {
            when (operation) {
                is NotificationStoreOperation.Add -> {
                    val messageReference = operation.messageReference
                    messageStore.setNewMessageState(
                        folderId = messageReference.folderId,
                        messageServerId = messageReference.uid,
                        newMessage = true,
                    )
                }
                is NotificationStoreOperation.Remove -> {
                    val messageReference = operation.messageReference
                    messageStore.setNewMessageState(
                        folderId = messageReference.folderId,
                        messageServerId = messageReference.uid,
                        newMessage = false,
                    )
                }
                else -> Unit
            }
        }
    }

    private fun clearNewMessageState(accountId: AccountId) {
        val messageStore = messageStoreManager.getMessageStore(accountId)
        messageStore.clearNewMessageState()
    }

    private fun clearNotificationStore(accountId: AccountId) {
        val notificationStore = notificationStoreProvider.getNotificationStore(accountId)
        notificationStore.clearNotifications()
    }
}
