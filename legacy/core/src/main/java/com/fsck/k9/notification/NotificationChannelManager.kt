package com.fsck.k9.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import app.k9mail.legacy.account.AccountManager
import app.k9mail.legacy.account.LegacyAccount
import java.util.concurrent.Executor
import net.thunderbird.feature.notification.NotificationLight
import net.thunderbird.feature.notification.NotificationSettings
import timber.log.Timber

class NotificationChannelManager(
    private val accountManager: AccountManager,
    private val backgroundExecutor: Executor,
    private val notificationManager: NotificationManager,
    private val resourceProvider: NotificationResourceProvider,
    private val notificationLightDecoder: NotificationLightDecoder,
) {
    val pushChannelId = "push"
    val miscellaneousChannelId = "misc"

    enum class ChannelType {
        MESSAGES,
        MISCELLANEOUS,
    }

    init {
        accountManager.addOnAccountsChangeListener(this::updateChannels)
    }

    fun updateChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        backgroundExecutor.execute {
            addGeneralChannels()

            val accounts = accountManager.getAccounts()

            removeChannelsForNonExistingOrChangedAccounts(notificationManager, accounts)
            addChannelsForAccounts(notificationManager, accounts)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun addGeneralChannels() {
        notificationManager.createNotificationChannel(getChannelPush())
        notificationManager.createNotificationChannel(getChannelMiscellaneous())
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun addChannelsForAccounts(
        notificationManager: NotificationManager,
        accounts: List<LegacyAccount>,
    ) {
        for (account in accounts) {
            val groupId = account.notificationChannelGroupId
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
        accounts: List<LegacyAccount>,
    ) {
        val accountUuids = accounts.map { it.uuid }.toSet()

        val groups = notificationManager.notificationChannelGroups
        for (group in groups) {
            val accountUuid = group.id.toAccountUuid()
            if (accountUuid !in accountUuids) {
                notificationManager.deleteNotificationChannelGroup(group.id)
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
    private fun getChannelMiscellaneous(): NotificationChannel {
        val channelName = resourceProvider.miscellaneousChannelName
        val channelDescription = resourceProvider.miscellaneousChannelDescription
        val importance = NotificationManager.IMPORTANCE_LOW

        return NotificationChannel(miscellaneousChannelId, channelName, importance).apply {
            description = channelDescription
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun getChannelMessages(account: LegacyAccount): NotificationChannel {
        val channelName = resourceProvider.messagesChannelName
        val channelId = getChannelIdFor(account, ChannelType.MESSAGES)
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        return NotificationChannel(channelId, channelName, importance).apply {
            description = resourceProvider.messagesChannelDescription
            group = account.uuid

            setPropertiesFrom(account)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun getChannelMiscellaneous(account: LegacyAccount): NotificationChannel {
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

    fun getChannelIdFor(account: LegacyAccount, channelType: ChannelType): String {
        return if (channelType == ChannelType.MESSAGES) {
            getMessagesChannelId(account, account.messagesNotificationChannelSuffix)
        } else {
            "miscellaneous_channel_${account.uuid}"
        }
    }

    private fun getMessagesChannelId(account: LegacyAccount, suffix: String): String {
        return "messages_channel_${account.uuid}$suffix"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getNotificationConfiguration(account: LegacyAccount): NotificationConfiguration {
        val channelId = getChannelIdFor(account, ChannelType.MESSAGES)
        val notificationChannel = notificationManager.getNotificationChannel(channelId)

        return NotificationConfiguration(
            sound = notificationChannel.sound,
            isBlinkLightsEnabled = notificationChannel.shouldShowLights(),
            lightColor = notificationChannel.lightColor,
            isVibrationEnabled = notificationChannel.shouldVibrate(),
            vibrationPattern = notificationChannel.vibrationPattern?.toList(),
        )
    }

    fun recreateMessagesNotificationChannel(account: LegacyAccount) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val oldChannelId = getChannelIdFor(account, ChannelType.MESSAGES)
        val oldNotificationChannel = notificationManager.getNotificationChannel(oldChannelId)

        if (oldNotificationChannel.matches(account)) {
            Timber.v("Not recreating NotificationChannel. The current one already matches the app's settings.")
            return
        }

        val newChannelVersion = account.messagesNotificationChannelVersion + 1
        val newChannelId = getMessagesChannelId(account, "_$newChannelVersion")
        val channelName = resourceProvider.messagesChannelName
        val importance = oldNotificationChannel.importance

        val newNotificationChannel = NotificationChannel(newChannelId, channelName, importance).apply {
            description = resourceProvider.messagesChannelDescription
            group = account.uuid

            copyPropertiesFrom(oldNotificationChannel)
            setPropertiesFrom(account)
        }

        Timber.v("Recreating NotificationChannel(%s => %s)", oldChannelId, newChannelId)
        Timber.v("Old NotificationChannel: %s", oldNotificationChannel)
        Timber.v("New NotificationChannel: %s", newNotificationChannel)
        notificationManager.createNotificationChannel(newNotificationChannel)

        // To avoid a race condition we first create the new NotificationChannel, point the Account to it,
        // then delete the old one.
        account.messagesNotificationChannelVersion = newChannelVersion
        notificationManager.deleteNotificationChannel(oldChannelId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun NotificationChannel.matches(account: LegacyAccount): Boolean {
        val systemLight = notificationLightDecoder.decode(
            isBlinkLightsEnabled = shouldShowLights(),
            lightColor = lightColor,
            accountColor = account.chipColor,
        )
        val notificationSettings = account.notificationSettings
        return sound == notificationSettings.ringtoneUri &&
            systemLight == notificationSettings.light &&
            shouldVibrate() == notificationSettings.vibration.isEnabled &&
            vibrationPattern.contentEquals(notificationSettings.vibration.systemPattern)
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
    private fun NotificationChannel.setPropertiesFrom(account: LegacyAccount) {
        val notificationSettings = account.notificationSettings

        if (notificationSettings.isRingEnabled) {
            setSound(notificationSettings.ringtone?.toUri(), Notification.AUDIO_ATTRIBUTES_DEFAULT)
        }

        notificationSettings.light.toColor(account.chipColor)?.let { lightColor ->
            this.lightColor = lightColor
        }
        val isLightEnabled = notificationSettings.light != NotificationLight.Disabled
        enableLights(isLightEnabled)

        vibrationPattern = notificationSettings.vibration.systemPattern
        enableVibration(notificationSettings.vibration.isEnabled)
    }

    private val LegacyAccount.notificationChannelGroupId: String
        get() = uuid

    private fun String.toAccountUuid(): String = this

    private val LegacyAccount.messagesNotificationChannelSuffix: String
        get() = messagesNotificationChannelVersion.let { version -> if (version == 0) "" else "_$version" }

    private val NotificationSettings.ringtoneUri: Uri?
        get() = if (isRingEnabled) ringtone?.toUri() else null
}

data class NotificationConfiguration(
    val sound: Uri?,
    val isBlinkLightsEnabled: Boolean,
    val lightColor: Int,
    val isVibrationEnabled: Boolean,
    val vibrationPattern: List<Long>?,
)
