package com.fsck.k9.helper;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;

/**
 * Create notifications using the new {@link android.app.Notification.Builder} class.
 */
@TargetApi(11)
public class NotificationBuilderApi11 extends NotificationBuilder {
    private Notification.Builder mBuilder;


    protected NotificationBuilderApi11(Context context) {
        super(context);
        mBuilder = new Notification.Builder(context);
    }

    @Override
    public void setSmallIcon(int icon) {
        mBuilder.setSmallIcon(icon);
    }

    @Override
    public void setWhen(long when) {
        mBuilder.setWhen(when);
    }

    @Override
    public void setTicker(CharSequence tickerText) {
        mBuilder.setTicker(tickerText);
    }

    @Override
    public void setContentTitle(CharSequence title) {
        mBuilder.setContentTitle(title);
    }

    @Override
    public void setContentText(CharSequence text) {
        mBuilder.setContentText(text);
    }

    @Override
    public void setContentIntent(PendingIntent intent) {
        mBuilder.setContentIntent(intent);
    }

    @Override
    public void setNumber(int number) {
        mBuilder.setNumber(number);
        mBuilder.setContentInfo("" + number);
    }

    @Override
    public void setOngoing(boolean ongoing) {
        mBuilder.setOngoing(ongoing);
    }

    @Override
    public void setAutoCancel(boolean autoCancel) {
        mBuilder.setAutoCancel(autoCancel);
    }

    @Override
    public void setSound(Uri sound) {
        mBuilder.setSound(sound, AudioManager.STREAM_NOTIFICATION);
    }

    @Override
    public void setVibrate(long[] pattern) {
        mBuilder.setVibrate(pattern);
    }

    @Override
    public void setLights(int argb, int onMs, int offMs) {
        mBuilder.setLights(argb, onMs, offMs);
    }

    @Override
    public Notification getNotification() {
        return mBuilder.getNotification();
    }
}
