package com.fsck.k9.activity

import com.fsck.k9.K9
import com.fsck.k9.UiDensity
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.preference.AppTheme
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.SplitViewMode
import net.thunderbird.core.preference.SubTheme

data class MessageListActivityConfig(
    val appTheme: AppTheme,
    val isShowUnifiedInbox: Boolean,
    val isShowMessageListStars: Boolean,
    val isShowCorrespondentNames: Boolean,
    val isMessageListSenderAboveSubject: Boolean,
    val isShowContactName: Boolean,
    val isChangeContactNameColor: Boolean,
    val isShowContactPicture: Boolean,
    val isColorizeMissingContactPictures: Boolean,
    val isUseBackgroundAsUnreadIndicator: Boolean,
    val isShowComposeButton: Boolean,
    val contactNameColor: Int,
    val messageViewTheme: SubTheme,
    val messageListPreviewLines: Int,
    val messageListDensity: UiDensity,
    val splitViewMode: SplitViewMode,
    val fontSizeMessageListSubject: Int,
    val fontSizeMessageListSender: Int,
    val fontSizeMessageListDate: Int,
    val fontSizeMessageListPreview: Int,
    val fontSizeMessageViewSender: Int,
    val fontSizeMessageViewRecipients: Int,
    val fontSizeMessageViewSubject: Int,
    val fontSizeMessageViewDate: Int,
    val fontSizeMessageViewContentAsPercent: Int,
    val swipeRightAction: SwipeAction,
    val swipeLeftAction: SwipeAction,
    val generalSettingsManager: GeneralSettingsManager,
) {

    companion object {
        fun create(
            generalSettingsManager: GeneralSettingsManager,
        ): MessageListActivityConfig {
            val settings = generalSettingsManager.getConfig()
            return MessageListActivityConfig(
                appTheme = settings.display.coreSettings.appTheme,
                isShowUnifiedInbox = settings.display.inboxSettings.isShowUnifiedInbox,
                isShowMessageListStars = settings.display.inboxSettings.isShowMessageListStars,
                isShowCorrespondentNames = settings.display.isShowCorrespondentNames,
                isMessageListSenderAboveSubject = settings.display.inboxSettings.isMessageListSenderAboveSubject,
                isShowContactName = settings.display.isShowContactName,
                isChangeContactNameColor = settings.display.isChangeContactNameColor,
                isShowContactPicture = settings.display.isShowContactPicture,
                isColorizeMissingContactPictures = settings.display.isColorizeMissingContactPictures,
                isUseBackgroundAsUnreadIndicator = settings.display.isUseBackgroundAsUnreadIndicator,
                isShowComposeButton = settings.display.inboxSettings.isShowComposeButtonOnMessageList,
                contactNameColor = K9.contactNameColor,
                messageViewTheme = settings.display.coreSettings.messageViewTheme,
                messageListPreviewLines = K9.messageListPreviewLines,
                messageListDensity = K9.messageListDensity,
                splitViewMode = settings.display.coreSettings.splitViewMode,
                fontSizeMessageListSubject = K9.fontSizes.messageListSubject,
                fontSizeMessageListSender = K9.fontSizes.messageListSender,
                fontSizeMessageListDate = K9.fontSizes.messageListDate,
                fontSizeMessageListPreview = K9.fontSizes.messageListPreview,
                fontSizeMessageViewSender = K9.fontSizes.messageViewSender,
                fontSizeMessageViewRecipients = K9.fontSizes.messageViewRecipients,
                fontSizeMessageViewSubject = K9.fontSizes.messageViewSubject,
                fontSizeMessageViewDate = K9.fontSizes.messageViewDate,
                fontSizeMessageViewContentAsPercent = K9.fontSizes.messageViewContentAsPercent,
                swipeRightAction = K9.swipeRightAction,
                swipeLeftAction = K9.swipeLeftAction,
                generalSettingsManager = generalSettingsManager,
            )
        }
    }
}
