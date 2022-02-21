package com.fsck.k9.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.fsck.k9.Account
import com.fsck.k9.NotificationLight
import com.fsck.k9.NotificationSettings
import com.fsck.k9.Preferences
import java.util.concurrent.Executor
import timber.log.Timber

class NotificationChannelManager(
    private val preferences: Preferences,
    private val backgroundExecutor: Executor,
    private val notificationManager: NotificationManager,
    private val resourceProvider: NotificationResourceProvider,
    private val notificationLightDecoder: NotificationLightDecoder
) {
    val pushChannelId = "push"

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
            addGeneralChannels()

            val accounts = preferences.accounts

            removeChannelsForNonExistingOrChangedAccounts(notificationManager, accounts)
            addChannelsForAccounts(notificationManager, accounts)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun addGeneralChannels() {
        notificationManager.createNotificationChannel(getChannelPush())
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
    private fun getChannelPush(): NotificationChannel {
        val channelName = resourceProvider.pushChannelName
        val channelDescription = resourceProvider.pushChannelDescription
        val importance = NotificationManager.IMPORTANCE_LOW

        return NotificationChannel(pushChannelId, channelName, importance).apply {
            description = channelDescription
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
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
        return if (channelType == ChannelType.MESSAGES) {
            "messages_channel_${account.uuid}${account.messagesNotificationChannelSuffix}"
        } else {
            "miscellaneous_channel_${account.uuid}"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getNotificationConfiguration(account: Account): NotificationConfiguration {
        val channelId = getChannelIdFor(account, ChannelType.MESSAGES)
        val notificationChannel = notificationManager.getNotificationChannel(channelId)

        return NotificationConfiguration(
            sound = notificationChannel.sound,
            isBlinkLightsEnabled = notificationChannel.shouldShowLights(),
            lightColor = notificationChannel.lightColor,
            isVibrationEnabled = notificationChannel.shouldVibrate(),
            vibrationPattern = notificationChannel.vibrationPattern?.toList()
        )
    }

    fun recreateMessagesNotificationChannel(account: Account) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val oldChannelId = getChannelIdFor(account, ChannelType.MESSAGES)
        val oldNotificationChannel = notificationManager.getNotificationChannel(oldChannelId)

        if (oldNotificationChannel.matches(account)) {
            Timber.v("Not recreating NotificationChannel. The current one already matches the app's settings.")
            return
        }

        notificationManager.deleteNotificationChannel(oldChannelId)

        account.incrementMessagesNotificationChannelVersion()

        val newChannelId = getChannelIdFor(account, ChannelType.MESSAGES)
        val channelName = resourceProvider.messagesChannelName
        val importance = oldNotificationChannel.importance

        val newNotificationChannel = NotificationChannel(newChannelId, channelName, importance).apply {
            description = resourceProvider.messagesChannelDescription
            group = account.uuid

            copyPropertiesFrom(oldNotificationChannel)
            copyPropertiesFrom(account)
        }

        Timber.v("Recreating NotificationChannel(%s => %s)", oldChannelId, newChannelId)
        Timber.v("Old NotificationChannel: %s", oldNotificationChannel)
        Timber.v("New NotificationChannel: %s", newNotificationChannel)
        notificationManager.createNotificationChannel(newNotificationChannel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun NotificationChannel.matches(account: Account): Boolean {
        val systemLight = notificationLightDecoder.decode(
            isBlinkLightsEnabled = shouldShowLights(),
            lightColor = lightColor,
            accountColor = account.chipColor
        )
        val notificationSettings = account.notificationSettings
        return sound == notificationSettings.ringtoneUri &&
            systemLight == notificationSettings.light &&
            shouldVibrate() == notificationSettings.isVibrateEnabled &&
            vibrationPattern.contentEquals(notificationSettings.vibrationPattern)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun NotificationChannel.copyPropertiesFrom(otherNotificationChannel: NotificationChannel) {
        setShowBadge(otherNotificationChannel.canShowBadge())
        setSound(otherNotificationChannel.sound, otherNotificationChannel.audioAttributes)
        enableVibration(otherNotificationChannel.shouldVibrate())
        enableLights(otherNotificationChannel.shouldShowLights())
        setBypassDnd(otherNotificationChannel.canBypassDnd())
        lockscreenVisibility = otherNotificationChannel.lockscreenVisibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setAllowBubbles(otherNotificationChannel.canBubble())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun NotificationChannel.copyPropertiesFrom(account: Account) {
        val notificationSettings = account.notificationSettings

        if (notificationSettings.isRingEnabled) {
            setSound(notificationSettings.ringtone?.toUri(), Notification.AUDIO_ATTRIBUTES_DEFAULT)
        }

        notificationSettings.light.toColor(account)?.let { lightColor ->
            this.lightColor = lightColor
        }
        val isLightEnabled = notificationSettings.light != NotificationLight.Disabled
        enableLights(isLightEnabled)

        vibrationPattern = notificationSettings.vibrationPattern
        enableVibration(notificationSettings.isVibrateEnabled)
    }

    private val Account.messagesNotificationChannelSuffix: String
        get() = messagesNotificationChannelVersion.let { version -> if (version == 0) "" else "_$version" }

    private val NotificationSettings.ringtoneUri: Uri?
        get() = if (isRingEnabled) ringtone?.toUri() else null
}

data class NotificationConfiguration(
    val sound: Uri?,
    val isBlinkLightsEnabled: Boolean,
    val lightColor: Int,
    val isVibrationEnabled: Boolean,
    val vibrationPattern: List<Long>?
)
