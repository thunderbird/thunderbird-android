package com.fsck.k9.activity

import com.fsck.k9.K9
import com.fsck.k9.preferences.AppTheme
import com.fsck.k9.preferences.GeneralSettingsManager
import com.fsck.k9.preferences.SubTheme

data class MessageListActivityAppearance(
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
    val contactNameColor: Int,
    val messageViewTheme: SubTheme,
    val messageListPreviewLines: Int,
    val splitViewMode: K9.SplitViewMode,
    val fontSizeMessageListSubject: Int,
    val fontSizeMessageListSender: Int,
    val fontSizeMessageListDate: Int,
    val fontSizeMessageListPreview: Int,
    val fontSizeMessageViewSender: Int,
    val fontSizeMessageViewTo: Int,
    val fontSizeMessageViewCC: Int,
    val fontSizeMessageViewBCC: Int,
    val fontSizeMessageViewAdditionalHeaders: Int,
    val fontSizeMessageViewSubject: Int,
    val fontSizeMessageViewDate: Int,
    val fontSizeMessageViewContentAsPercent: Int
) {

    companion object {
        fun create(generalSettingsManager: GeneralSettingsManager): MessageListActivityAppearance {
            val settings = generalSettingsManager.getSettings()
            return MessageListActivityAppearance(
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
                contactNameColor = K9.contactNameColor,
                messageViewTheme = settings.messageViewTheme,
                messageListPreviewLines = K9.messageListPreviewLines,
                splitViewMode = K9.splitViewMode,
                fontSizeMessageListSubject = K9.fontSizes.messageListSubject,
                fontSizeMessageListSender = K9.fontSizes.messageListSender,
                fontSizeMessageListDate = K9.fontSizes.messageListDate,
                fontSizeMessageListPreview = K9.fontSizes.messageListPreview,
                fontSizeMessageViewSender = K9.fontSizes.messageViewSender,
                fontSizeMessageViewTo = K9.fontSizes.messageViewTo,
                fontSizeMessageViewCC = K9.fontSizes.messageViewCC,
                fontSizeMessageViewBCC = K9.fontSizes.messageViewBCC,
                fontSizeMessageViewAdditionalHeaders = K9.fontSizes.messageViewAdditionalHeaders,
                fontSizeMessageViewSubject = K9.fontSizes.messageViewSubject,
                fontSizeMessageViewDate = K9.fontSizes.messageViewDate,
                fontSizeMessageViewContentAsPercent = K9.fontSizes.messageViewContentAsPercent
            )
        }
    }
}
