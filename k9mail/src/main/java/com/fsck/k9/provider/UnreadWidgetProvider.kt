package com.fsck.k9.provider

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.fsck.k9.R
import com.fsck.k9.widget.unread.UnreadWidgetData
import com.fsck.k9.widget.unread.UnreadWidgetConfigurationActivity
import com.fsck.k9.widget.unread.UnreadWidgetRepository
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

class UnreadWidgetProvider : AppWidgetProvider(), KoinComponent {
    private val repository: UnreadWidgetRepository by inject()


    /**
     * Called when one or more widgets need to be updated.
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            val properties = repository.getWidgetProperties(widgetId)
            properties?.let {
                updateWidget(context, appWidgetManager, it)
            }
        }
    }

    /**
     * Called when a widget instance is deleted.
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            repository.deleteWidgetConfiguration(appWidgetId)
        }
    }

    companion object {
        private const val MAX_COUNT = 9999

        /**
         * Trigger update for all of our unread widgets.
         */
        @JvmStatic
        fun updateUnreadCount(context: Context) {
            val appContext = context.applicationContext
            val appWidgetManager = AppWidgetManager.getInstance(appContext)

            val thisWidget = ComponentName(appContext, UnreadWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            updateWidgets(context, *widgetIds)
        }

        @JvmStatic
        fun updateWidget(context: Context, widgetId: Int) {
            updateWidgets(context, widgetId)
        }

        private fun updateWidgets(context: Context, vararg widgetIds: Int) {
            val updateIntent = Intent(context, UnreadWidgetProvider::class.java)
            updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds.toTypedArray())

            context.sendBroadcast(updateIntent)
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, properties: UnreadWidgetData) {
            val remoteViews = RemoteViews(context.packageName, R.layout.unread_widget_layout)

            val appWidgetId = properties.appWidgetId
            var clickIntent: Intent? = null

            try {
                clickIntent = properties.getClickIntent(context)
                val unreadCount = properties.getUnreadCount(context)

                if (unreadCount <= 0) {
                    // Hide TextView for unread count if there are no unread messages.
                    remoteViews.setViewVisibility(R.id.unread_count, View.GONE)
                } else {
                    remoteViews.setViewVisibility(R.id.unread_count, View.VISIBLE)

                    val displayCount = if (unreadCount <= MAX_COUNT) unreadCount.toString() else "$MAX_COUNT+"
                    remoteViews.setTextViewText(R.id.unread_count, displayCount)
                }

                remoteViews.setTextViewText(R.id.title, properties.getTitle(context))
            } catch (e: Exception) {
                Timber.e(e, "Error getting widget configuration")
            }

            if (clickIntent == null) {
                // If the widget configuration couldn't be loaded we open the configuration
                // activity when the user clicks the widget.
                clickIntent = Intent(context, UnreadWidgetConfigurationActivity::class.java)
                clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            clickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val pendingIntent = PendingIntent.getActivity(context, appWidgetId, clickIntent, 0)
            remoteViews.setOnClickPendingIntent(R.id.unread_widget_layout, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }
}
