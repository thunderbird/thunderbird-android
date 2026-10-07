package com.fsck.k9.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.storage.profile.ProfileDto
import net.thunderbird.legacy.logging.Log
import net.thunderbird.feature.notification.NotificationLight
import net.thunderbird.feature.notification.NotificationSettings

class NotificationChannelManager(
    private val accountManager: LegacyAccountManager,
    private val backgroundExecutor: Executor,
    private val notificationManager: NotificationManager,
    private val resourceProvider: NotificationResourceProvider,
    private val notificationLightDecoder: NotificationLightDecoder,
    coroutineScope: CoroutineScope = GlobalScope,
) {
    val pushChannelId = "push"
    val miscellaneousChannelId = "misc"

    enum class ChannelType {
        MESSAGES,
        MISCELLANEOUS,
    }

    init {
        coroutineScope.launch {
            accountManager.observeAll()
                .distinctUntilChanged()
                .collect {
                    updateChannels()
                }
        }
    }

    fun updateChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        backgroundExecutor.execute {
            addGeneralChannels()

            val accounts = accountManager.findAll()

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
            val groupId = account.id.toString()
            val group = NotificationChannelGroup(groupId, account.profile.name)

            val channelMessages = getChannelMessages(account)
            val channelMiscellaneous = getChannelMiscellaneous(account.id, account.messagesNotificationChannelVersion)

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
        val accountIds = accounts.map { it.id.toString() }.toSet()

        val groups = notificationManager.notificationChannelGroups
        for (group in groups) {
            if (group.id !in accountIds) {
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
        val channelId = getChannelIdFor(account.id, ChannelType.MESSAGES, account.messagesNotificationChannelVersion)
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        return NotificationChannel(channelId, channelName, importance).apply {
            description = resourceProvider.messagesChannelDescription
            group = account.id.toString()

            setPropertiesFrom(account.notificationSettings, account.profile)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun getChannelMiscellaneous(accountId: AccountId, channelVersion: Int): NotificationChannel {
        val channelName = resourceProvider.miscellaneousChannelName
        val channelDescription = resourceProvider.miscellaneousChannelDescription
        val channelId =
            getChannelIdFor(accountId, ChannelType.MISCELLANEOUS, channelVersion)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channelGroupId = accountId.toString()

        val miscellaneousChannel = NotificationChannel(channelId, channelName, importance)
        miscellaneousChannel.description = channelDescription
        miscellaneousChannel.group = channelGroupId

        return miscellaneousChannel
    }

    fun getChannelIdFor(accountId: AccountId, channelType: ChannelType, channelVersion: Int): String {
        return if (channelType == ChannelType.MESSAGES) {
            getMessagesChannelId(accountId, channelVersion)
        } else {
            "miscellaneous_channel_$accountId"
        }
    }

    private fun getMessagesChannelId(accountId: AccountId, channelVersion: Int): String {
        val suffix = channelVersion.let { version -> if (version == 0) "" else "_$version" }
        return "messages_channel_$accountId$suffix"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getNotificationConfiguration(accountId: AccountId, channelVersion: Int): NotificationConfiguration {
        val channelId = getChannelIdFor(accountId, ChannelType.MESSAGES, channelVersion)
        val notificationChannel = notificationManager.getNotificationChannel(channelId)

        return NotificationConfiguration(
            sound = notificationChannel.sound,
            isBlinkLightsEnabled = notificationChannel.shouldShowLights(),
            lightColor = notificationChannel.lightColor,
            isVibrationEnabled = notificationChannel.shouldVibrate(),
            vibrationPattern = notificationChannel.vibrationPattern?.toList(),
        )
    }

    fun recreateMessagesNotificationChannel(accountId: AccountId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val account = accountManager.findById(accountId) ?: return

        val oldChannelId = getChannelIdFor(account.id, ChannelType.MESSAGES, account.messagesNotificationChannelVersion)
        val oldNotificationChannel = notificationManager.getNotificationChannel(oldChannelId)

        if (!oldNotificationChannel.matches(account)) {
            val newChannelVersion = account.messagesNotificationChannelVersion + 1
            val newChannelId = getMessagesChannelId(account.id, newChannelVersion)
            val channelName = resourceProvider.messagesChannelName
            val importance = oldNotificationChannel.importance

            val newNotificationChannel = NotificationChannel(newChannelId, channelName, importance).apply {
                description = resourceProvider.messagesChannelDescription
                group = account.id.toString()

                copyPropertiesFrom(oldNotificationChannel)
                setPropertiesFrom(account.notificationSettings, account.profile)
            }

            Log.v("Recreating NotificationChannel(%s => %s)", oldChannelId, newChannelId)
            Log.v("Old NotificationChannel: %s", oldNotificationChannel)
            Log.v("New NotificationChannel: %s", newNotificationChannel)
            notificationManager.createNotificationChannel(newNotificationChannel)

            // To avoid a race condition we first create the new NotificationChannel, point the Account to it,
            // then delete the old one.
            // TDOD: this should be changed to use a notification settings repository
            updateAccountChannelVersion(account.id, newChannelVersion)

            notificationManager.deleteNotificationChannel(oldChannelId)
        } else {
            Log.v("Not recreating NotificationChannel. The current one already matches the app's settings.")
        }
    }

    private fun updateAccountChannelVersion(accountId: AccountId, newChannelVersion: Int) {
        val currentAccount = accountManager.findById(accountId)

        if (currentAccount != null) {
            accountManager.updateSync(currentAccount.copy(messagesNotificationChannelVersion = newChannelVersion))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun NotificationChannel.matches(account: LegacyAccount): Boolean {
        val systemLight = notificationLightDecoder.decode(
            isBlinkLightsEnabled = shouldShowLights(),
            lightColor = lightColor,
            accountColor = account.profile.color,
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
    private fun NotificationChannel.setPropertiesFrom(settings: NotificationSettings, profile: ProfileDto) {
        if (settings.isRingEnabled) {
            setSound(settings.ringtone?.toUri(), Notification.AUDIO_ATTRIBUTES_DEFAULT)
        }

        settings.light.toColor(profile.color)?.let { lightColor ->
            this.lightColor = lightColor
        }
        val isLightEnabled = settings.light != NotificationLight.Disabled
        enableLights(isLightEnabled)

        vibrationPattern = settings.vibration.systemPattern
        enableVibration(settings.vibration.isEnabled)
    }

    private fun String.toAccountUuid(): String = this

    private val LegacyAccount.notificationChannelGroupId: String
        get() = id.toString()

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
