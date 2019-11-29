package com.fsck.k9.widget.unread

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.fsck.k9.provider.UnreadWidgetProvider

class UnreadWidgetUpdater(private val context: Context) {
    private val appWidgetManager = AppWidgetManager.getInstance(context)

    fun updateAll() {
        val thisWidget = ComponentName(context, UnreadWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        updateWidgets(context, widgetIds)
    }

    fun update(widgetId: Int) {
        updateWidgets(context, intArrayOf(widgetId))
    }

    private fun updateWidgets(context: Context, widgetIds: IntArray) {
        val updateIntent = Intent(context, UnreadWidgetProvider::class.java)
        updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)

        context.sendBroadcast(updateIntent)
    }
}
