package com.fsck.k9.activity

import app.k9mail.legacy.preferences.AppTheme
import app.k9mail.legacy.preferences.GeneralSettingsManager
import app.k9mail.legacy.preferences.SubTheme
import com.fsck.k9.K9
import com.fsck.k9.SwipeAction
import com.fsck.k9.UiDensity

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
    val splitViewMode: K9.SplitViewMode,
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
) {

    companion object {
        fun create(generalSettingsManager: GeneralSettingsManager): MessageListActivityConfig {
            val settings = generalSettingsManager.getSettings()
            return MessageListActivityConfig(
                appTheme = settings.appTheme,
                isShowUnifiedInbox = K9.isShowUnifiedInbox,
                isShowMessageListStars = K9.isShowMessageListStars,
                isShowCorrespondentNames = K9.isShowCorrespondentNames,
                isMessageListSenderAboveSubject = K9.isMessageListSenderAboveSubject,
                isShowContactName = K9.isShowContactName,
                isChangeContactNameColor = K9.isChangeContactNameColor,
                isShowContactPicture = K9.isShowContactPicture,
                isColorizeMissingContactPictures = K9.isColorizeMissingContactPictures,
                isUseBackgroundAsUnreadIndicator = K9.isUseBackgroundAsUnreadIndicator,
                isShowComposeButton = K9.isShowComposeButtonOnMessageList,
                contactNameColor = K9.contactNameColor,
                messageViewTheme = settings.messageViewTheme,
                messageListPreviewLines = K9.messageListPreviewLines,
                messageListDensity = K9.messageListDensity,
                splitViewMode = K9.splitViewMode,
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
            )
        }
    }
}
