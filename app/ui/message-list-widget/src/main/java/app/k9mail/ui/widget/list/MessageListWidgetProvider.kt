package app.k9mail.ui.widget.list

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.PendingIntentCompat
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.activity.MessageList
import com.fsck.k9.activity.MessageList.Companion.intentDisplaySearch
import com.fsck.k9.search.SearchAccount.Companion.createUnifiedInboxAccount
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.fsck.k9.ui.R as UiR

open class MessageListWidgetProvider : AppWidgetProvider(), KoinComponent {
    private val messageListWidgetManager: MessageListWidgetManager by inject()

    override fun onEnabled(context: Context) {
        messageListWidgetManager.onWidgetAdded()
    }

    override fun onDisabled(context: Context) {
        messageListWidgetManager.onWidgetRemoved()
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.message_list_widget_layout)

        views.setTextViewText(R.id.folder, context.getString(UiR.string.integrated_inbox_title))

        val intent = Intent(context, MessageListWidgetService::class.java)
        views.setRemoteAdapter(R.id.listView, intent)

        val viewAction = viewActionTemplatePendingIntent(context)
        views.setPendingIntentTemplate(R.id.listView, viewAction)

        val composeAction = composeActionPendingIntent(context)
        views.setOnClickPendingIntent(R.id.new_message, composeAction)

        val headerClickAction = viewUnifiedInboxPendingIntent(context)
        views.setOnClickPendingIntent(R.id.top_controls, headerClickAction)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun viewActionTemplatePendingIntent(context: Context): PendingIntent {
        val intent = MessageList.actionDisplayMessageTemplateIntent(
            context,
            openInUnifiedInbox = true,
            messageViewOnly = true,
        )
        return PendingIntentCompat.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT, true)!!
    }

    private fun viewUnifiedInboxPendingIntent(context: Context): PendingIntent {
        val unifiedInboxAccount = createUnifiedInboxAccount()
        val intent = intentDisplaySearch(
            context = context,
            search = unifiedInboxAccount.relatedSearch,
            noThreading = true,
            newTask = true,
            clearTop = true,
        )
        return PendingIntentCompat.getActivity(context, -1, intent, PendingIntent.FLAG_UPDATE_CURRENT, false)!!
    }

    private fun composeActionPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MessageCompose::class.java).apply {
            action = MessageCompose.ACTION_COMPOSE
        }
        return PendingIntentCompat.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT, false)!!
    }
}
