package net.thunderbird.core.preference

import kotlinx.coroutines.flow.Flow

/**
 * Retrieve and modify general settings.
 *
 */
interface GeneralSettingsManager : PreferenceManager<GeneralSettings> {
    @Deprecated(
        message = "Use PreferenceManager<GeneralSettings>.getConfig() instead",
        replaceWith = ReplaceWith(
            expression = "getConfig()",
        ),
    )
    fun getSettings(): GeneralSettings

    @Deprecated(
        message = "Use PreferenceManager<GeneralSettings>.getConfigFlow() instead",
        replaceWith = ReplaceWith(
            expression = "getConfigFlow()",
        ),
    )
    fun getSettingsFlow(): Flow<GeneralSettings>
}
