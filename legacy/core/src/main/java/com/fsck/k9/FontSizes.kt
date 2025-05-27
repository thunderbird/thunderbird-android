package com.fsck.k9

import android.util.TypedValue
import android.widget.TextView
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor

/**
 * Manage font size of the information displayed in the message list and in the message view.
 */
class FontSizes {
    var messageListSubject: Int
    var messageListSender: Int
    var messageListDate: Int
    var messageListPreview: Int
    var messageViewAccountName: Int
    var messageViewSender: Int
    var messageViewRecipients: Int
    var messageViewSubject: Int
    var messageViewDate: Int
    var messageViewContentAsPercent: Int
    var messageComposeInput: Int

    init {
        messageListSubject = FONT_DEFAULT
        messageListSender = FONT_DEFAULT
        messageListDate = FONT_DEFAULT
        messageListPreview = FONT_DEFAULT
        messageViewAccountName = FONT_DEFAULT
        messageViewSender = FONT_DEFAULT
        messageViewRecipients = FONT_DEFAULT
        messageViewSubject = FONT_DEFAULT
        messageViewDate = FONT_DEFAULT
        messageComposeInput = MEDIUM
        messageViewContentAsPercent = DEFAULT_CONTENT_SIZE_IN_PERCENT
    }

    fun save(editor: StorageEditor) {
        with(editor) {
            putInt(MESSAGE_LIST_SUBJECT, messageListSubject)
            putInt(MESSAGE_LIST_SENDER, messageListSender)
            putInt(MESSAGE_LIST_DATE, messageListDate)
            putInt(MESSAGE_LIST_PREVIEW, messageListPreview)

            putInt(MESSAGE_VIEW_ACCOUNT_NAME, messageViewAccountName)
            putInt(MESSAGE_VIEW_SENDER, messageViewSender)
            putInt(MESSAGE_VIEW_RECIPIENTS, messageViewRecipients)
            putInt(MESSAGE_VIEW_SUBJECT, messageViewSubject)
            putInt(MESSAGE_VIEW_DATE, messageViewDate)
            putInt(MESSAGE_VIEW_CONTENT_PERCENT, messageViewContentAsPercent)

            putInt(MESSAGE_COMPOSE_INPUT, messageComposeInput)
        }
    }

    fun load(storage: Storage) {
        messageListSubject = storage.getInt(MESSAGE_LIST_SUBJECT, messageListSubject)
        messageListSender = storage.getInt(MESSAGE_LIST_SENDER, messageListSender)
        messageListDate = storage.getInt(MESSAGE_LIST_DATE, messageListDate)
        messageListPreview = storage.getInt(MESSAGE_LIST_PREVIEW, messageListPreview)

        messageViewAccountName = storage.getInt(MESSAGE_VIEW_ACCOUNT_NAME, messageViewAccountName)
        messageViewSender = storage.getInt(MESSAGE_VIEW_SENDER, messageViewSender)
        messageViewRecipients = storage.getInt(MESSAGE_VIEW_RECIPIENTS, messageViewRecipients)
        messageViewSubject = storage.getInt(MESSAGE_VIEW_SUBJECT, messageViewSubject)
        messageViewDate = storage.getInt(MESSAGE_VIEW_DATE, messageViewDate)

        loadMessageViewContentPercent(storage)

        messageComposeInput = storage.getInt(MESSAGE_COMPOSE_INPUT, messageComposeInput)
    }

    private fun loadMessageViewContentPercent(storage: Storage) {
        messageViewContentAsPercent = storage.getInt(MESSAGE_VIEW_CONTENT_PERCENT, DEFAULT_CONTENT_SIZE_IN_PERCENT)
    }

    // This, arguably, should live somewhere in a view class, but since we call it from activities, fragments
    // and views, where isn't exactly clear.
    fun setViewTextSize(view: TextView, fontSize: Int) {
        if (fontSize != FONT_DEFAULT) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize.toFloat())
        }
    }

    companion object {
        private const val MESSAGE_LIST_SUBJECT = "fontSizeMessageListSubject"
        private const val MESSAGE_LIST_SENDER = "fontSizeMessageListSender"
        private const val MESSAGE_LIST_DATE = "fontSizeMessageListDate"
        private const val MESSAGE_LIST_PREVIEW = "fontSizeMessageListPreview"
        private const val MESSAGE_VIEW_ACCOUNT_NAME = "fontSizeMessageViewAccountName"
        private const val MESSAGE_VIEW_SENDER = "fontSizeMessageViewSender"
        private const val MESSAGE_VIEW_RECIPIENTS = "fontSizeMessageViewTo"
        private const val MESSAGE_VIEW_SUBJECT = "fontSizeMessageViewSubject"
        private const val MESSAGE_VIEW_DATE = "fontSizeMessageViewDate"
        private const val MESSAGE_VIEW_CONTENT_PERCENT = "fontSizeMessageViewContentPercent"
        private const val MESSAGE_COMPOSE_INPUT = "fontSizeMessageComposeInput"

        private const val DEFAULT_CONTENT_SIZE_IN_PERCENT = 100

        const val FONT_DEFAULT = -1 // Don't force-reset the size of this setting
        const val FONT_10SP = 10
        const val FONT_12SP = 12
        const val SMALL = 14 // ?android:attr/textAppearanceSmall
        const val FONT_16SP = 16
        const val MEDIUM = 18 // ?android:attr/textAppearanceMedium
        const val FONT_20SP = 20
        const val LARGE = 22 // ?android:attr/textAppearanceLarge
    }
}
