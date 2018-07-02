package com.fsck.k9.widget.list;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.AccountList;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.K9PreferenceActivity;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.search.SearchAccount;


public class MessageListWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_UPDATE_MESSAGE_LIST = "UPDATE_MESSAGE_LIST";


    public static void triggerMessageListWidgetUpdate(Context context) {
        Context appContext = context.getApplicationContext();
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(appContext);
        ComponentName widget = new ComponentName(appContext, MessageListWidgetProvider.class);
        int[] widgetIds = widgetManager.getAppWidgetIds(widget);

        Intent intent = new Intent(context, MessageListWidgetProvider.class);
        intent.setAction(ACTION_UPDATE_MESSAGE_LIST);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        context.sendBroadcast(intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
    //----------------------------------change LB 29.06.2018-------------------------------------
    private PendingIntent enterpostActionPendingIntent(Context context) {
        Intent intent = new Intent(context, MessageList.class);
        intent.setAction(Intent.ACTION_VIEW);

        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
//--------------------------------------------------------------------------------------------

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.message_list_widget_layout);

        Intent intent = new Intent(context, MessageListWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.listView, intent);

        PendingIntent viewAction = viewActionTemplatePendingIntent(context);
        views.setPendingIntentTemplate(R.id.listView, viewAction);

        PendingIntent composeAction = composeActionPendingIntent(context);
        views.setOnClickPendingIntent(R.id.new_message, composeAction);

        //change LB 29.06.2018
        PendingIntent enterpostAction = enterpostActionPendingIntent(context);
        views.setOnClickPendingIntent(R.id.enter_post, enterpostAction);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        if (action.equals(ACTION_UPDATE_MESSAGE_LIST)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.listView);
        }
    }

    private PendingIntent viewActionTemplatePendingIntent(Context context) {
        Intent intent = new Intent(context, MessageList.class);
        intent.setAction(Intent.ACTION_VIEW);

        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent composeActionPendingIntent(Context context) {
        Intent intent = new Intent(context, MessageCompose.class);
        intent.setAction(MessageCompose.ACTION_COMPOSE);

        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
