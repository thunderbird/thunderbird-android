package com.fsck.k9.notification;

import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.fsck.k9.K9;
import com.fsck.k9.R;

import java.util.ArrayList;

// The default notification for Android silently ignores any actions
// attached to it after the first three.  This class fixes that so
// we can have more then three buttons.
public class ExtendedNotificationBuilder extends NotificationCompat.Builder {
    protected static final int actions = Resources.getSystem().getIdentifier("actions", "id", "android");
    protected static final int action0 = Resources.getSystem().getIdentifier("action0", "id", "android");
    protected static final int notification_action = Resources.getSystem().getIdentifier("notification_action", "layout", "android");
    protected static final int notification_action_tombstone = Resources.getSystem().getIdentifier("notification_action_tombstone", "layout", "android");

    public ExtendedNotificationBuilder(Context context) {
        super(context);
    }

    public Notification build() {
        Notification notification = super.build();
        int N = mActions.size();
        for (int i=3; i<N; i++) {
            final RemoteViews button = generateActionButton(mActions.get(i));
            notification.bigContentView.addView(actions, button);
        }
        return notification;
    }

    private RemoteViews generateActionButton(NotificationCompat.Action action) {
        final boolean tombstone = (action.actionIntent == null);
        RemoteViews button = new RemoteViews(mContext.getPackageName(),
                tombstone ? notification_action
                        : notification_action_tombstone);
        button.setTextViewCompoundDrawables(action0, action.icon, 0, 0, 0);
        button.setTextViewText(action0, action.title);
        if (!tombstone) {
            button.setOnClickPendingIntent(action0, action.actionIntent);
        }
        button.setContentDescription(action0, action.title);
        return button;
    }
}
