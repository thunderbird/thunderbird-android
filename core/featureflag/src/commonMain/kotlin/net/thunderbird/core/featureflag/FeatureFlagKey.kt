package net.thunderbird.core.featureflag

@JvmInline
value class FeatureFlagKey(val key: String) {
    companion object Keys {
        val DisplayInAppNotifications = "display_in_app_notifications".toFeatureFlagKey()
        val UseNotificationSenderForSystemNotifications =
            "use_notification_sender_for_system_notifications".toFeatureFlagKey()
    }
}

fun String.toFeatureFlagKey(): FeatureFlagKey = FeatureFlagKey(this)
