package app.k9mail.feature.widget.message.list

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import androidx.core.content.ContextCompat
import app.k9mail.legacy.account.SortType
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.K9
import com.fsck.k9.activity.MessageList
import net.thunderbird.feature.search.LocalSearch
import net.thunderbird.feature.search.SearchAccount
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("TooManyFunctions")
internal class MessageListRemoteViewFactory(private val context: Context) : RemoteViewsFactory, KoinComponent {
    private val messageListLoader: MessageListLoader by inject()
    private val coreResourceProvider: CoreResourceProvider by inject()

    private lateinit var unifiedInboxSearch: LocalSearch

    private var messageListItems = emptyList<MessageListItem>()
    private var senderAboveSubject = false
    private var readTextColor = 0
    private var unreadTextColor = 0

    override fun onCreate() {
        unifiedInboxSearch = SearchAccount.createUnifiedInboxAccount(
            unifiedInboxTitle = coreResourceProvider.searchUnifiedInboxTitle(),
            unifiedInboxDetail = coreResourceProvider.searchUnifiedInboxDetail(),
        ).relatedSearch

        senderAboveSubject = K9.isMessageListSenderAboveSubject
        readTextColor = ContextCompat.getColor(context, R.color.message_list_widget_text_read)
        unreadTextColor = ContextCompat.getColor(context, R.color.message_list_widget_text_unread)
    }

    override fun onDataSetChanged() {
        loadMessageList()
    }

    private fun loadMessageList() {
        // TODO: Use same sort order that is used for the Unified Inbox inside the app
        val messageListConfig = MessageListConfig(
            search = unifiedInboxSearch,
            showingThreadedList = K9.isThreadedViewEnabled,
            sortType = SortType.SORT_DATE,
            sortAscending = false,
            sortDateAscending = false,
        )

        messageListItems = messageListLoader.getMessageList(messageListConfig)
    }

    override fun onDestroy() = Unit

    override fun getCount(): Int = messageListItems.size

    override fun getViewAt(position: Int): RemoteViews {
        val remoteView = RemoteViews(context.packageName, R.layout.message_list_widget_list_item)

        val item = messageListItems[position]

        val displayName = if (item.isRead) item.displayName else bold(item.displayName)
        val subject = if (item.isRead) item.subject else bold(item.subject)

        if (senderAboveSubject) {
            remoteView.setTextViewText(R.id.sender, displayName)
            remoteView.setTextViewText(R.id.mail_subject, subject)
        } else {
            remoteView.setTextViewText(R.id.sender, subject)
            remoteView.setTextViewText(R.id.mail_subject, displayName)
        }

        remoteView.setTextViewText(R.id.mail_date, item.displayDate)
        remoteView.setTextViewText(R.id.mail_preview, item.preview)

        if (item.threadCount > 1) {
            remoteView.setTextViewText(R.id.thread_count, item.threadCount.toString())
            remoteView.setInt(R.id.thread_count, "setVisibility", View.VISIBLE)
        } else {
            remoteView.setInt(R.id.thread_count, "setVisibility", View.GONE)
        }

        val textColor = getTextColor(item)
        remoteView.setTextColor(R.id.sender, textColor)
        remoteView.setTextColor(R.id.mail_subject, textColor)
        remoteView.setTextColor(R.id.mail_date, textColor)
        remoteView.setTextColor(R.id.mail_preview, textColor)

        if (item.hasAttachments) {
            remoteView.setInt(R.id.attachment, "setVisibility", View.VISIBLE)
        } else {
            remoteView.setInt(R.id.attachment, "setVisibility", View.GONE)
        }

        val intent = MessageList.actionDisplayMessageTemplateFillIntent(item.messageReference)
        remoteView.setOnClickFillInIntent(R.id.mail_list_item, intent)

        remoteView.setInt(R.id.chip, "setBackgroundColor", item.accountColor)

        return remoteView
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.message_list_widget_list_item_loading).apply {
            // Set the text here instead of in the layout so the app language override is used
            setTextViewText(R.id.loadingText, context.getString(R.string.message_list_widget_list_item_loading))
        }
    }

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long = messageListItems[position].uniqueId

    override fun hasStableIds(): Boolean = true

    private fun bold(text: String): CharSequence {
        return SpannableString(text).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, text.length, 0)
        }
    }

    private fun getTextColor(messageListItem: MessageListItem): Int {
        return if (messageListItem.isRead) readTextColor else unreadTextColor
    }
}
