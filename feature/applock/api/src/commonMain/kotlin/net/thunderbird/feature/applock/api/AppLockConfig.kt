package net.thunderbird.feature.applock.api

/**
 * Configuration settings for app lock.
 *
 * @property isEnabled Whether biometric/device authentication is enabled.
 * @property timeoutMillis Timeout in milliseconds after which re-authentication is required
 *                         when the app returns from background. Use 0 for immediate re-authentication.
 */
data class AppLockConfig(
    val isEnabled: Boolean = DEFAULT_ENABLED,
    val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) {
    companion object {
        /**
         * Default: App lock is disabled.
         */
        const val DEFAULT_ENABLED = false

        /**
         * Default timeout: 0 (immediate re-authentication required when returning from background).
         */
        const val DEFAULT_TIMEOUT_MILLIS = 0L
    }
}
