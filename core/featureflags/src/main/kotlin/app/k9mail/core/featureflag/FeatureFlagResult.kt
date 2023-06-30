package app.k9mail.core.featureflag

sealed interface FeatureFlagResult {
    object Enabled : FeatureFlagResult
    object Disabled : FeatureFlagResult
    object Unavailable : FeatureFlagResult

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
}
