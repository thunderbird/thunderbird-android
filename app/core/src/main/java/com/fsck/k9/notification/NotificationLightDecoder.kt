package com.fsck.k9.notification

import com.fsck.k9.NotificationLight

/**
 * Converts the "blink lights" values read from a `NotificationChannel` into [NotificationLight].
 */
class NotificationLightDecoder {
    fun decode(isBlinkLightsEnabled: Boolean, lightColor: Int, accountColor: Int): NotificationLight {
        if (!isBlinkLightsEnabled) return NotificationLight.Disabled

        return when (lightColor.rgb) {
            accountColor.rgb -> NotificationLight.AccountColor
            0xFFFFFF -> NotificationLight.White
            0xFF0000 -> NotificationLight.Red
            0x00FF00 -> NotificationLight.Green
            0x0000FF -> NotificationLight.Blue
            0xFFFF00 -> NotificationLight.Yellow
            0x00FFFF -> NotificationLight.Cyan
            0xFF00FF -> NotificationLight.Magenta
            else -> NotificationLight.SystemDefaultColor
        }
    }

    private val Int.rgb
        get() = this and 0x00FFFFFF
}
