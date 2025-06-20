package net.thunderbird.core.preference

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
    val backgroundSync: BackgroundSync,
    val showRecentChanges: Boolean,
    val appTheme: AppTheme,
    val messageViewTheme: SubTheme,
    val messageComposeTheme: SubTheme,
    val fixedMessageViewTheme: Boolean,
    val isShowUnifiedInbox: Boolean,
    val isShowStarredCount: Boolean,
    val isShowMessageListStars: Boolean,
    val isShowAnimations: Boolean,
    val isShowCorrespondentNames: Boolean,
    val shouldShowSetupArchiveFolderDialog: Boolean,
    val isMessageListSenderAboveSubject: Boolean,
    val isShowContactName: Boolean,
    val isShowContactPicture: Boolean,
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
