package com.fsck.k9.ui.messagelist

import com.fsck.k9.FontSizes
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListDateTimeFormat
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity

data class MessageListAppearance(
    val fontSizes: FontSizes,
    val previewLines: Int,
    val stars: Boolean,
    val senderAboveSubject: Boolean,
    val showContactPicture: Boolean,
    val showingThreadedList: Boolean,
    val backGroundAsReadIndicator: Boolean,
    /**
     * Whether to show an account color indicator on the left side of the message item.
     */
    val showAccountIndicator: Boolean,
    val density: UiDensity,
    val dateTimeFormat: MessageListDateTimeFormat,
) : Parcelable
