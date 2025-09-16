package net.thunderbird.core.featureflag

sealed interface FeatureFlagResult {
    data object Enabled : FeatureFlagResult
    data object Disabled : FeatureFlagResult
    data object Unavailable : FeatureFlagResult

    fun <T> whenEnabledOrNot(
        onEnabled: () -> T,
        onDisabledOrUnavailable: () -> T,
    ): T = when (this) {
        is Enabled -> onEnabled()
        is Disabled, Unavailable -> onDisabledOrUnavailable()
    }

    fun onEnabled(action: () -> Unit): FeatureFlagResult {
        if (this is Enabled) {
            action()
        }

        return this
    }

    fun onDisabled(action: () -> Unit): FeatureFlagResult {
        if (this is Disabled) {
            action()
        }

        return this
    }

    fun onUnavailable(action: () -> Unit): FeatureFlagResult {
        if (this is Unavailable) {
            action()
        }

        return this
    }

    fun onDisabledOrUnavailable(action: () -> Unit): FeatureFlagResult {
        if (this is Disabled || this is Unavailable) {
            action()
        }

        return this
    }

    fun isEnabled(): Boolean = this is Enabled
    fun isDisabled(): Boolean = this is Disabled
    fun isUnavailable(): Boolean = this is Unavailable
    fun isDisabledOrUnavailable(): Boolean = this is Disabled || this is Unavailable
}
