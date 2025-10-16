package com.fsck.k9.notification

import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.feature.notification.NotificationSettings

/**
 * Converts the [NotificationConfiguration] read from a `NotificationChannel` into a [NotificationSettings] instance.
 */
class NotificationConfigurationConverter(
    private val notificationLightDecoder: NotificationLightDecoder,
    private val notificationVibrationDecoder: NotificationVibrationDecoder,
) {
    fun convert(
        account: LegacyAccountDto,
        notificationConfiguration: NotificationConfiguration,
    ): NotificationSettings {
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
