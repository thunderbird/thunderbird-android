package net.thunderbird.core.preference

import net.thunderbird.core.preference.privacy.PrivacySettings

/**
 * Stores a snapshot of the app's general settings.
 *
 * When adding a setting here, make sure to also add it in these places:
 * - [GeneralSettingsManager] (write function)
 * - [RealGeneralSettingsManager.loadGeneralSettings]
 * - [RealGeneralSettingsManager.writeSettings]
 * - [GeneralSettingsDescriptions]
 */
// TODO: Move over settings from K9
data class GeneralSettings(
    val backgroundSync: BackgroundSync = BackgroundSync.ALWAYS,
    val showRecentChanges: Boolean = true,
    val appTheme: AppTheme = AppTheme.FOLLOW_SYSTEM,
    val messageViewTheme: SubTheme = SubTheme.USE_GLOBAL,
    val messageComposeTheme: SubTheme = SubTheme.USE_GLOBAL,
    val fixedMessageViewTheme: Boolean = true,
    val isShowUnifiedInbox: Boolean = false,
    val isShowStarredCount: Boolean = false,
    val isShowMessageListStars: Boolean = true,
    val isShowAnimations: Boolean = true,
    val isShowCorrespondentNames: Boolean = true,
    val shouldShowSetupArchiveFolderDialog: Boolean = true,
    val isMessageListSenderAboveSubject: Boolean = false,
    val isShowContactName: Boolean = false,
    val isShowContactPicture: Boolean = true,
    val isChangeContactNameColor: Boolean = true,
    val isColorizeMissingContactPictures: Boolean = false,
    val isUseBackgroundAsUnreadIndicator: Boolean = false,
    val isShowComposeButtonOnMessageList: Boolean = true,
    val isThreadedViewEnabled: Boolean = true,
    val isUseMessageViewFixedWidthFont: Boolean = false,
    val isAutoFitWidth: Boolean = true,
    val quietTimeEnds: String = "7:00",
    val quietTimeStarts: String = "21:00",
    val isQuietTimeEnabled: Boolean = false,
    val isQuietTime: Boolean = false,
    val privacy: PrivacySettings = PrivacySettings(),
)

enum class BackgroundSync {
    ALWAYS,
    NEVER,
    FOLLOW_SYSTEM_AUTO_SYNC,
}

enum class AppTheme {
    LIGHT,
    DARK,
    FOLLOW_SYSTEM,
}

enum class SubTheme {
    LIGHT,
    DARK,
    USE_GLOBAL,
}
