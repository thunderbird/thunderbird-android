package app.k9mail.legacy.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Retrieve and modify general settings.
 *
 * TODO: Add more settings as needed.
 */
interface GeneralSettingsManager {
    fun getSettings(): GeneralSettings
    fun getSettingsFlow(): Flow<GeneralSettings>

    fun setShowRecentChanges(showRecentChanges: Boolean)
    fun setAppTheme(appTheme: AppTheme)
    fun setMessageViewTheme(subTheme: SubTheme)
    fun setMessageComposeTheme(subTheme: SubTheme)
    fun setFixedMessageViewTheme(fixedMessageViewTheme: Boolean)

    fun addListener(listener: GeneralSettingsChangeListener)
    fun removeListener(listener: GeneralSettingsChangeListener)
}
