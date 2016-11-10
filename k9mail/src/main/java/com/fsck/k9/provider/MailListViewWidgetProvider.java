package com.fsck.k9.provider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.MailListViewWidgetConfiguration;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.service.MailListViewWidgetService;

/**
 * Implementation of App Widget functionality.
 */
public class MailListViewWidgetProvider extends AppWidgetProvider {
    public static final String PACKAGE_NAME = "com.fsck.k9";

    public static String ACTION_VIEW_MAIL_ITEM = PACKAGE_NAME + ".provider.ACTION_VIEW_MAIL_ITEM";
    public static String ACTION_COMPOSE_EMAIL = PACKAGE_NAME + ".provider.ACTION_COMPOSE_EMAIL";


    private static String account;

    /**
     * When a new email is received or deleted call this method to broadcast an intent
     * in order to update the listview of the widget
     * @param context
     */
    public static void updateMailViewList(Context context) {
        Context appContext = context.getApplicationContext();
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(appContext);
        ComponentName widget = new ComponentName(appContext, MailListViewWidgetProvider.class);
        int [] widgetIds = widgetManager.getAppWidgetIds(widget);

        Intent intent = new Intent(context, MailListViewWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        context.sendBroadcast(intent);
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, String acc) {

        CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.mail_list_view_widget_layout);
        views.setRemoteAdapter(R.id.listView, new Intent(context, MailListViewWidgetService.class));
        views.setTextViewText(R.id.folder, "INBOX");
        Intent intent = new Intent(context, MailListViewWidgetService.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
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
        // Add intent for composing a new message
        views.setOnClickPendingIntent(R.id.new_message, newMailIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        String acc;
        for (int i = 0; i < N; i++) {
            acc = MailListViewWidgetConfiguration.getAccountUuid(context, appWidgetIds[i]);
            account = acc;
            updateAppWidget(context, appWidgetManager, appWidgetIds[i], acc);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        // an intent from an item on the listview. get the uri and launch the MessageList activity
        if (intent.getAction().equals(ACTION_VIEW_MAIL_ITEM)) {
            Intent viewMailIntent = new Intent(context, MessageList.class);
            viewMailIntent.setAction(Intent.ACTION_VIEW);
            viewMailIntent.setData(Uri.parse(intent.getStringExtra(AppWidgetManager.EXTRA_CUSTOM_INFO)));
            viewMailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(viewMailIntent);
        }
        // an intent from updateMailViewList() method. Call AppWidgetManager#notifyAppWidgetViewDataChanged()
        // to update the listview
        if (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            AppWidgetManager.getInstance(context)
                    .notifyAppWidgetViewDataChanged(intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS),
                                                    R.id.listView);
        }

        if (intent.getAction().equals(ACTION_COMPOSE_EMAIL)) {
            Intent newMessage = new Intent(context, MessageCompose.class)
                    .putExtra(MessageCompose.EXTRA_ACCOUNT, account)
                    .setAction(Intent.ACTION_VIEW)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(newMessage);
        }
    }
    
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int widgId : appWidgetIds) {
            MailListViewWidgetConfiguration.deleteWidgetConfiguration(context, widgId);
        }
    }
}

