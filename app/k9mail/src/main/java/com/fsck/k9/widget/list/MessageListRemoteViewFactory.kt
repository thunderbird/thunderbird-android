package com.fsck.k9.widget.list

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Binder
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import androidx.core.content.ContextCompat
import androidx.core.database.getLongOrNull
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.external.MessageProvider
import com.fsck.k9.helper.getBoolean
import com.fsck.k9.helper.map
import java.util.Calendar
import java.util.Locale
import com.fsck.k9.ui.R as UiR

class MessageListRemoteViewFactory(private val context: Context) : RemoteViewsFactory {
    private val calendar: Calendar = Calendar.getInstance()

    private var mailItems = emptyList<MailItem>()
    private var senderAboveSubject = false
    private var readTextColor = 0
    private var unreadTextColor = 0

    override fun onCreate() {
        senderAboveSubject = K9.isMessageListSenderAboveSubject
        readTextColor = ContextCompat.getColor(context, R.color.message_list_widget_text_read)
        unreadTextColor = ContextCompat.getColor(context, R.color.message_list_widget_text_unread)
    }

    override fun onDataSetChanged() {
        val identityToken = Binder.clearCallingIdentity()
        try {
            loadMessageList()
        } finally {
            Binder.restoreCallingIdentity(identityToken)
        }
    }

    private fun loadMessageList() {
        val contentResolver = context.contentResolver
        val unifiedInboxUri = MessageProvider.CONTENT_URI.buildUpon().appendPath("inbox_messages").build()

        mailItems = contentResolver.query(unifiedInboxUri, MAIL_LIST_PROJECTIONS, null, null, null)?.use { cursor ->
            cursor.map {
                MailItem(
                    sender = cursor.getString(0),
                    date = cursor.getLongOrNull(1) ?: 0L,
                    subject = cursor.getString(2),
                    preview = cursor.getString(3),
                    unread = cursor.getBoolean(4),
                    hasAttachment = cursor.getBoolean(5),
                    uri = Uri.parse(cursor.getString(6)),
                    color = cursor.getInt(7)
                )
            }
        } ?: emptyList()
    }

    override fun onDestroy() = Unit

    override fun getCount(): Int = mailItems.size

    override fun getViewAt(position: Int): RemoteViews {
        val remoteView = RemoteViews(context.packageName, R.layout.message_list_widget_list_item)

        val item = mailItems[position]

        val sender = if (item.unread) bold(item.sender) else item.sender
        val subject = if (item.unread) bold(item.subject) else item.subject

        if (senderAboveSubject) {
            remoteView.setTextViewText(R.id.sender, sender)
            remoteView.setTextViewText(R.id.mail_subject, subject)
        } else {
            remoteView.setTextViewText(R.id.sender, subject)
            remoteView.setTextViewText(R.id.mail_subject, sender)
        }

        remoteView.setTextViewText(R.id.mail_date, formatDate(item.date))
        remoteView.setTextViewText(R.id.mail_preview, item.preview)

        val textColor = getTextColor(item)
        remoteView.setTextColor(R.id.sender, textColor)
        remoteView.setTextColor(R.id.mail_subject, textColor)
        remoteView.setTextColor(R.id.mail_date, textColor)
        remoteView.setTextColor(R.id.mail_preview, textColor)

        if (item.hasAttachment) {
            remoteView.setInt(R.id.attachment, "setVisibility", View.VISIBLE)
        } else {
            remoteView.setInt(R.id.attachment, "setVisibility", View.GONE)
        }

        val intent = Intent().apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data = item.uri
        }
        remoteView.setOnClickFillInIntent(R.id.mail_list_item, intent)

        remoteView.setInt(R.id.chip, "setBackgroundColor", item.color)

        return remoteView
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.message_list_widget_loading).apply {
            setTextViewText(R.id.loadingText, context.getString(UiR.string.mail_list_widget_loading))
            setViewVisibility(R.id.loadingText, View.VISIBLE)
        }
    }

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun bold(text: String): CharSequence {
        return SpannableString(text).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, text.length, 0)
        }
    }

    private fun formatDate(date: Long): String {
        calendar.timeInMillis = date
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())

        return String.format("%d %s", dayOfMonth, month)
    }

    private fun getTextColor(mailItem: MailItem): Int {
        return if (mailItem.unread) unreadTextColor else readTextColor
    }

    companion object {
        private val MAIL_LIST_PROJECTIONS = arrayOf(
            MessageProvider.MessageColumns.SENDER,
            MessageProvider.MessageColumns.SEND_DATE,
            MessageProvider.MessageColumns.SUBJECT,
            MessageProvider.MessageColumns.PREVIEW,
            MessageProvider.MessageColumns.UNREAD,
            MessageProvider.MessageColumns.HAS_ATTACHMENTS,
            MessageProvider.MessageColumns.URI,
            MessageProvider.MessageColumns.ACCOUNT_COLOR
        )
    }
}

private class MailItem(
    val sender: String,
    val date: Long,
    val subject: String,
    val preview: String,
    val unread: Boolean,
    val hasAttachment: Boolean,
    val uri: Uri,
    val color: Int
)
