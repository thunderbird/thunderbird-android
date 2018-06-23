package com.fsck.k9.widget.unread

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.fsck.k9.core.R
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

class UnreadWidgetProvider : AppWidgetProvider(), KoinComponent {
    private val repository: UnreadWidgetRepository by inject()


    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            val widgetData = repository.getWidgetData(widgetId)
            widgetData?.let {
                updateWidget(context, appWidgetManager, it)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            repository.deleteWidgetConfiguration(appWidgetId)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, data: UnreadWidgetData) {
        val remoteViews = RemoteViews(context.packageName, R.layout.unread_widget_layout)

        val appWidgetId = data.configuration.appWidgetId
        var clickIntent: Intent? = null

        try {
            clickIntent = data.clickIntent
            val unreadCount = data.unreadCount

            if (unreadCount <= 0) {
                // Hide TextView for unread count if there are no unread messages.
                remoteViews.setViewVisibility(R.id.unread_count, View.GONE)
            } else {
                remoteViews.setViewVisibility(R.id.unread_count, View.VISIBLE)

                val displayCount = if (unreadCount <= MAX_COUNT) unreadCount.toString() else "$MAX_COUNT+"
                remoteViews.setTextViewText(R.id.unread_count, displayCount)
            }

            remoteViews.setTextViewText(R.id.title, data.title)
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

    companion object {
        private const val MAX_COUNT = 9999
    }
}
