package app.k9mail.feature.widget.unread

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

class UnreadWidgetUpdater(
    private val context: Context,
    private val config: UnreadWidgetConfig,
) {
    private val appWidgetManager = AppWidgetManager.getInstance(context)

    fun updateAll() {
        val thisWidget = ComponentName(context, config.providerClass)
        val widgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        updateWidgets(context, widgetIds)
    }

    fun update(widgetId: Int) {
        updateWidgets(context, intArrayOf(widgetId))
    }

    private fun updateWidgets(context: Context, widgetIds: IntArray) {
        val updateIntent = Intent(context, config.providerClass)
        updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)

        context.sendBroadcast(updateIntent)
    }
}
