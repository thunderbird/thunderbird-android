package app.k9mail.ui.widget.list

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.activity.MessageList
import com.fsck.k9.activity.MessageList.Companion.intentDisplaySearch
import com.fsck.k9.helper.PendingIntentCompat.FLAG_IMMUTABLE
import com.fsck.k9.helper.PendingIntentCompat.FLAG_MUTABLE
import com.fsck.k9.search.SearchAccount.Companion.createUnifiedInboxAccount
import com.fsck.k9.ui.R as UiR

open class MessageListWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.message_list_widget_layout)

        views.setTextViewText(R.id.folder, context.getString(UiR.string.integrated_inbox_title))

        val intent = Intent(context, MessageListWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.listView, intent)

        val viewAction = viewActionTemplatePendingIntent(context)
        views.setPendingIntentTemplate(R.id.listView, viewAction)

        val composeAction = composeActionPendingIntent(context)
        views.setOnClickPendingIntent(R.id.new_message, composeAction)

        val headerClickAction = viewUnifiedInboxPendingIntent(context)
        views.setOnClickPendingIntent(R.id.top_controls, headerClickAction)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_UPDATE_MESSAGE_LIST) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)

            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listView)
        }
    }

    private fun viewActionTemplatePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MessageList::class.java).apply {
            action = Intent.ACTION_VIEW
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or FLAG_MUTABLE)
    }

    private fun viewUnifiedInboxPendingIntent(context: Context): PendingIntent {
        val unifiedInboxAccount = createUnifiedInboxAccount()
        val intent = intentDisplaySearch(
            context = context,
            search = unifiedInboxAccount.relatedSearch,
            noThreading = true,
            newTask = true,
            clearTop = true
        )

        return PendingIntent.getActivity(context, -1, intent, PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    private fun composeActionPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MessageCompose::class.java).apply {
            action = MessageCompose.ACTION_COMPOSE
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
    }

    companion object {
        private const val ACTION_UPDATE_MESSAGE_LIST = "UPDATE_MESSAGE_LIST"

        fun triggerMessageListWidgetUpdate(context: Context) {
            val appContext = context.applicationContext
            val widgetManager = AppWidgetManager.getInstance(appContext)

            val widget = ComponentName(appContext, MessageListWidgetProvider::class.java)
            val widgetIds = widgetManager.getAppWidgetIds(widget)
            val intent = Intent(context, MessageListWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_MESSAGE_LIST
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            }

            context.sendBroadcast(intent)
        }
    }
}
