package com.fsck.k9.provider;

import com.fsck.k9.R;
import com.fsck.k9.activity.UnreadWidgetConfiguration;
import com.fsck.k9.helper.UnreadWidgetProperties;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import timber.log.Timber;
import android.view.View;
import android.widget.RemoteViews;

public class UnreadWidgetProvider extends AppWidgetProvider {
    private static final int MAX_COUNT = 9999;

    /**
     * Trigger update for all of our unread widgets.
     *
     * @param context
     *         The {@code Context} object to use for the broadcast intent.
     */
    public static void updateUnreadCount(Context context) {
        Context appContext = context.getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(appContext);

        ComponentName thisWidget = new ComponentName(appContext, UnreadWidgetProvider.class);
        int[] widgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        Intent intent = new Intent(context, UnreadWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);

        context.sendBroadcast(intent);
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager,
                                    UnreadWidgetProperties properties) {

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.unread_widget_layout);

        int appWidgetId = properties.getAppWidgetId();
        Intent clickIntent = null;
        int unreadCount = 0;

        try {
            clickIntent = properties.getClickIntent(context);
            unreadCount = properties.getUnreadCount(context);

            if (unreadCount <= 0) {
                // Hide TextView for unread count if there are no unread messages.
                remoteViews.setViewVisibility(R.id.unread_count, View.GONE);
            } else {
                remoteViews.setViewVisibility(R.id.unread_count, View.VISIBLE);

                String displayCount = (unreadCount <= MAX_COUNT) ?
                        String.valueOf(unreadCount) : String.valueOf(MAX_COUNT) + "+";
                remoteViews.setTextViewText(R.id.unread_count, displayCount);
            }

            remoteViews.setTextViewText(R.id.title, properties.getTitle(context));
        } catch (Exception e) {
            Timber.e(e, "Error getting widget configuration");
        }


        if (clickIntent == null) {
            // If the widget configuration couldn't be loaded we open the configuration
            // activity when the user clicks the widget.
            clickIntent = new Intent(context, UnreadWidgetConfiguration.class);
            clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        }
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId,
                clickIntent, 0);

        remoteViews.setOnClickPendingIntent(R.id.unread_widget_layout, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }


    /**
     * Called when one or more widgets need to be updated.
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            UnreadWidgetProperties properties = UnreadWidgetConfiguration.getWidgetProperties(context, widgetId);
            updateWidget(context, appWidgetManager, properties);
        }
    }

    /**
     * Called when a widget instance is deleted.
     */
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            UnreadWidgetConfiguration.deleteWidgetConfiguration(context, appWidgetId);
        }
    }
}
