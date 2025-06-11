package net.thunderbird.core.preferences

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
    fun setIsShowUnifiedInbox(isShowUnifiedInbox: Boolean)
    fun setIsShowStarredCount(isShowStarredCount: Boolean)
    fun setIsShowMessageListStars(isShowMessageListStars: Boolean)
    fun setIsShowAnimations(isShowAnimations: Boolean)
}
