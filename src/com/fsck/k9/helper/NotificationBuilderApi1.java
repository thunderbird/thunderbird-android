package com.fsck.k9.helper;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;

/**
 * Create notifications using the now deprecated {@link Notification} constructor.
 */
public class NotificationBuilderApi1 extends NotificationBuilder {
    private int mSmallIcon;
    private long mWhen;
    private CharSequence mTickerText;
    private CharSequence mContentText;
    private CharSequence mContentTitle;
    private PendingIntent mContentIntent;
    private int mNumber;
    private boolean mOngoing;
    private boolean mAutoCancel;
    private Uri mSoundUri;
    private long[] mVibrationPattern;
    private int mLedColor;
    private int mLedOnMS;
    private int mLedOffMS;
    private boolean mBlinkLed;


    protected NotificationBuilderApi1(Context context) {
        super(context);
    }

    @Override
    public void setSmallIcon(int icon) {
        mSmallIcon = icon;
    }

    @Override
    public void setWhen(long when) {
        mWhen = when;
    }

    @Override
    public void setTicker(CharSequence tickerText) {
        mTickerText = tickerText;
    }

    @Override
    public void setContentTitle(CharSequence title) {
        mContentTitle = title;
    }

    @Override
    public void setContentText(CharSequence text) {
        mContentText = text;
    }

    @Override
    public void setContentIntent(PendingIntent intent) {
        mContentIntent = intent;
    }

    @Override
    public void setNumber(int number) {
        mNumber = number;
    }

    @Override
    public void setOngoing(boolean ongoing) {
        mOngoing = ongoing;
    }

    @Override
    public void setAutoCancel(boolean autoCancel) {
        mAutoCancel = autoCancel;
    }

    @Override
    public void setSound(Uri sound) {
        mSoundUri = sound;
    }

    @Override
    public void setVibrate(long[] pattern) {
        mVibrationPattern = pattern;
    }

    @Override
    public void setLights(int argb, int onMs, int offMs) {
        mBlinkLed = true;
        mLedColor = argb;
        mLedOnMS = onMs;
        mLedOffMS = offMs;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Notification getNotification() {
        Notification notification = new Notification(mSmallIcon, mTickerText, mWhen);
        notification.number = mNumber;
        notification.setLatestEventInfo(mContext, mContentTitle, mContentText, mContentIntent);

        if (mSoundUri != null) {
            notification.sound = mSoundUri;
            notification.audioStreamType = AudioManager.STREAM_NOTIFICATION;
        }

        if (mVibrationPattern != null) {
            notification.vibrate = mVibrationPattern;
        }

        if (mBlinkLed) {
            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
            notification.ledARGB = mLedColor;
            notification.ledOnMS = mLedOnMS;
            notification.ledOffMS = mLedOffMS;
        }

        if (mAutoCancel) {
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
        }

        if (mOngoing) {
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
        }

        return notification;
    }
}
