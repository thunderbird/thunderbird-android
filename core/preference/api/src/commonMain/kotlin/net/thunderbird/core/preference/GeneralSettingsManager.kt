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

    // TODO(#9432): Remove all setters below in favour of PreferenceManager<T>.update(updater)
    fun setShowRecentChanges(showRecentChanges: Boolean)
    fun setAppTheme(appTheme: AppTheme)
    fun setMessageViewTheme(subTheme: SubTheme)
    fun setMessageComposeTheme(subTheme: SubTheme)
    fun setFixedMessageViewTheme(fixedMessageViewTheme: Boolean)
    fun setIsShowUnifiedInbox(isShowUnifiedInbox: Boolean)
    fun setIsShowStarredCount(isShowStarredCount: Boolean)
    fun setIsShowMessageListStars(isShowMessageListStars: Boolean)
    fun setIsShowAnimations(isShowAnimations: Boolean)
    fun setIsShowCorrespondentNames(isShowCorrespondentNames: Boolean)
    fun setSetupArchiveShouldNotShowAgain(shouldShowSetupArchiveFolderDialog: Boolean)
    fun setIsMessageListSenderAboveSubject(isMessageListSenderAboveSubject: Boolean)
    fun setIsShowContactName(isShowContactName: Boolean)
    fun setIsShowContactPicture(isShowContactPicture: Boolean)
    fun setIsChangeContactNameColor(isChangeContactNameColor: Boolean)
    fun setIsColorizeMissingContactPictures(isColorizeMissingContactPictures: Boolean)
    fun setIsUseBackgroundAsUnreadIndicator(isUseBackgroundAsUnreadIndicator: Boolean)
    fun setIsShowComposeButtonOnMessageList(isShowComposeButtonOnMessageList: Boolean)
    fun setIsThreadedViewEnabled(isThreadedViewEnabled: Boolean)
    fun setIsUseMessageViewFixedWidthFont(isUseMessageViewFixedWidthFont: Boolean)
    fun setIsAutoFitWidth(isAutoFitWidth: Boolean)
    fun setQuietTimeEnds(quietTimeEnds: String)
    fun setQuietTimeStarts(quietTimeStarts: String)
    fun setIsQuietTimeEnabled(isQuietTimeEnabled: Boolean)
}
