package app.k9mail.feature.widget.unread

import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.PendingIntentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.thunderbird.core.logging.Logger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val TAG = "BaseUnreadWidgetProvider"

/**
 * Unread widget provider that displays the number of unread messages on the user's home screen.
 *
 * The concrete implementation of this class must be added to the app's manifest.
 *
 * The manifest entry should look like this:
 *
 * ```
 * <manifest>
 *     <application>
 *         <receiver
 *             android:name="app.k9mail.feature.widget.unread.UnreadWidgetProvider"
 *             android:label="@string/unread_widget_label"
 *             android:enabled="@bool/home_screen_widgets_enabled"
 *             android:exported="false">
 *             <intent-filter>
 *                 <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
 *             </intent-filter>
 *             <meta-data
 *                 android:name="android.appwidget.provider"
 *                 android:resource="@xml/unread_widget_info" />
 *         </receiver>
 *     </application>
 * </manifest>
 * ```
 *
 * IMPORTANT: The concrete implementation of this class that is exposed via the manifest and must have a fully
 *            qualified class name that can't ever be changed. Otherwise widgets created with older versions of the app
 *            will stop working.
 */
abstract class BaseUnreadWidgetProvider : AppWidgetProvider(), KoinComponent {
    private val repository: UnreadWidgetRepository by inject()
    private val logger: Logger by inject()
    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()

        widgetScope.launch {
            updateWidgets(context, appWidgetManager, appWidgetIds)
            pendingResult.finish()
        }
    }

    private suspend fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            val widgetData = repository.getWidgetData(widgetId) ?: continue
            updateWidget(context, appWidgetManager, widgetData)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            repository.deleteWidgetConfiguration(appWidgetId)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        data: UnreadWidgetData,
    ) {
        val remoteViews = RemoteViews(context.packageName, R.layout.unread_widget_layout)

        val appWidgetId = data.configuration.appWidgetId

        val clickIntent = try {
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

            data.clickIntent
        } catch (e: Exception) {
            logger.error(TAG, e) { "Error getting widget configuration for widget ID $appWidgetId" }
            null
        } ?: Intent(context, UnreadWidgetConfigurationActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntentCompat.getActivity(
            context,
            appWidgetId,
            clickIntent,
            FLAG_UPDATE_CURRENT,
            true,
        )
        remoteViews.setOnClickPendingIntent(R.id.unread_widget_layout, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }

    companion object {
        private const val MAX_COUNT = 9999
    }
}
