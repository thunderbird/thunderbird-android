package com.fsck.k9.notification

import app.k9mail.legacy.notification.NotificationSettings
import com.fsck.k9.Account

/**
 * Converts the [NotificationConfiguration] read from a `NotificationChannel` into a [NotificationSettings] instance.
 */
class NotificationConfigurationConverter(
    private val notificationLightDecoder: NotificationLightDecoder,
    private val notificationVibrationDecoder: NotificationVibrationDecoder,
) {
    fun convert(account: Account, notificationConfiguration: NotificationConfiguration): NotificationSettings {
        val light = notificationLightDecoder.decode(
            isBlinkLightsEnabled = notificationConfiguration.isBlinkLightsEnabled,
            lightColor = notificationConfiguration.lightColor,
            accountColor = account.chipColor,
        )

        val vibration = notificationVibrationDecoder.decode(
            isVibrationEnabled = notificationConfiguration.isVibrationEnabled,
            systemPattern = notificationConfiguration.vibrationPattern,
        )

        return NotificationSettings(
            isRingEnabled = notificationConfiguration.sound != null,
            ringtone = notificationConfiguration.sound?.toString(),
            light = light,
            vibration = vibration,
        )
    }
}
