package com.fsck.k9.widget.list;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.fsck.k9.R;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.MessageList;


public class MailListViewWidgetProvider extends AppWidgetProvider {
    public static final String PACKAGE_NAME = "com.fsck.k9";
    public static String ACTION_VIEW_MAIL_ITEM = PACKAGE_NAME + ".provider.ACTION_VIEW_MAIL_ITEM";
    public static String ACTION_COMPOSE_EMAIL = PACKAGE_NAME + ".provider.ACTION_COMPOSE_EMAIL";
    public static String ACTION_UPDATE_MESSAGE_LIST = PACKAGE_NAME + ".provider.ACTION_UPDATE_MESSAGE_LIST";


    public static void updateMailViewList(Context context) {
        Context appContext = context.getApplicationContext();
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(appContext);
        ComponentName widget = new ComponentName(appContext, MailListViewWidgetProvider.class);
        int[] widgetIds = widgetManager.getAppWidgetIds(widget);

        Intent intent = new Intent(context, MailListViewWidgetProvider.class);
        intent.setAction(ACTION_UPDATE_MESSAGE_LIST);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        context.sendBroadcast(intent);
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.mail_list_view_widget_layout);

        views.setTextViewText(R.id.folder, context.getString(R.string.integrated_inbox_title));

        Intent intent = new Intent(context, MailListViewWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.listView, intent);

        Intent clickIntent = new Intent(context, MailListViewWidgetProvider.class);
        clickIntent.setPackage(PACKAGE_NAME);
        clickIntent.setAction(ACTION_VIEW_MAIL_ITEM);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, MessageList.REQUEST_MASK_PENDING_INTENT,
                clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.listView, pendingIntent);

        Intent composeIntent = new Intent(context, MailListViewWidgetProvider.class);
        composeIntent.setPackage(PACKAGE_NAME);
        composeIntent.setAction(ACTION_COMPOSE_EMAIL);

        PendingIntent newMailIntent = PendingIntent.getBroadcast(context, 0, composeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.new_message, newMailIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (intent.getAction().equals(ACTION_VIEW_MAIL_ITEM)) {
            Intent viewMailIntent = new Intent(context, MessageList.class);
            viewMailIntent.setAction(Intent.ACTION_VIEW);
            viewMailIntent.setData(Uri.parse(intent.getStringExtra(AppWidgetManager.EXTRA_CUSTOM_INFO)));
            viewMailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(viewMailIntent);
        } else if (intent.getAction().equals(ACTION_UPDATE_MESSAGE_LIST)) {
            AppWidgetManager.getInstance(context)
                    .notifyAppWidgetViewDataChanged(intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS),
                            R.id.listView);
        } else if (intent.getAction().equals(ACTION_COMPOSE_EMAIL)) {
            Intent newMessage = new Intent(context, MessageCompose.class)
                    .setAction(MessageCompose.ACTION_COMPOSE)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(newMessage);
        }
    }
}
