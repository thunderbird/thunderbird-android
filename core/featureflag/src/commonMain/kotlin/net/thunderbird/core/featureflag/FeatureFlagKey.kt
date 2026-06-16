package net.thunderbird.core.featureflag

@JvmInline
value class FeatureFlagKey(val key: String) {
    companion object Keys {
        /**
         * DO NOT ADD NEW FEATURE FLAGS HERE.
         *
         * New feature flags should be added to an object in the `:api` module of the feature
         * they belong to, to avoid tight coupling.
         * See `docs/architecture/feature-flags.md` for more details.
         */
        val DisplayInAppNotifications = "display_in_app_notifications".toFeatureFlagKey()
        val UseNotificationSenderForSystemNotifications =
            "use_notification_sender_for_system_notifications".toFeatureFlagKey()
    }
}

fun String.toFeatureFlagKey(): FeatureFlagKey = FeatureFlagKey(this)
