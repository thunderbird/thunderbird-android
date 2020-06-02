package com.fsck.k9.notification

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import java.util.concurrent.Executor

class NotificationChannelManager(
    private val preferences: Preferences,
    private val backgroundExecutor: Executor,
    private val notificationManager: NotificationManager,
    private val resourceProvider: NotificationResourceProvider
) {

    enum class ChannelType {
        MESSAGES, MISCELLANEOUS
    }

    init {
        preferences.addOnAccountsChangeListener(this::updateChannels)
    }

    fun updateChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        backgroundExecutor.execute {
            val accounts = preferences.accounts

            removeChannelsForNonExistingOrChangedAccounts(notificationManager, accounts)
            addChannelsForAccounts(notificationManager, accounts)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun addChannelsForAccounts(
        notificationManager: NotificationManager,
        accounts: List<Account>
    ) {
        for (account in accounts) {
            val groupId = account.uuid
            val group = NotificationChannelGroup(groupId, account.displayName)

            val channelMessages = getChannelMessages(account)
            val channelMiscellaneous = getChannelMiscellaneous(account)

            notificationManager.createNotificationChannelGroup(group)
            notificationManager.createNotificationChannel(channelMessages)
            notificationManager.createNotificationChannel(channelMiscellaneous)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun removeChannelsForNonExistingOrChangedAccounts(
        notificationManager: NotificationManager,
        accounts: List<Account>
    ) {
        val existingAccounts = HashMap<String, Account>()
        for (account in accounts) {
            existingAccounts[account.uuid] = account
        }

        val groups = notificationManager.notificationChannelGroups
        for (group in groups) {
            val groupId = group.id

            var shouldDelete = false
            if (!existingAccounts.containsKey(groupId)) {
                shouldDelete = true
            } else if (existingAccounts[groupId]?.displayName != group.name.toString()) {
                // There is no way to change group names. Deleting group, so it is re-generated.
                shouldDelete = true
            }

            if (shouldDelete) {
                notificationManager.deleteNotificationChannelGroup(groupId)
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun getChannelMessages(account: Account): NotificationChannel {
        val channelName = resourceProvider.messagesChannelName
        val channelDescription = resourceProvider.messagesChannelDescription
        val channelId = getChannelIdFor(account, ChannelType.MESSAGES)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channelGroupId = account.uuid

        val messagesChannel = NotificationChannel(channelId, channelName, importance)
        messagesChannel.description = channelDescription
        messagesChannel.group = channelGroupId

        return messagesChannel
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun getChannelMiscellaneous(account: Account): NotificationChannel {
        val channelName = resourceProvider.miscellaneousChannelName
        val channelDescription = resourceProvider.miscellaneousChannelDescription
        val channelId = getChannelIdFor(account, ChannelType.MISCELLANEOUS)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channelGroupId = account.uuid

        val miscellaneousChannel = NotificationChannel(channelId, channelName, importance)
        miscellaneousChannel.description = channelDescription
        miscellaneousChannel.group = channelGroupId

        return miscellaneousChannel
    }

    fun getChannelIdFor(account: Account, channelType: ChannelType): String {
        val accountUuid = account.uuid

        return if (channelType == ChannelType.MESSAGES) {
            "messages_channel_$accountUuid"
        } else {
            "miscellaneous_channel_$accountUuid"
        }
    }
}
