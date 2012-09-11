package com.fsck.k9.helper;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;

/**
 * Helper class to create system notifications
 *
 * @see NotificationBuilderApi1
 * @see NotificationBuilderApi11
 */
public abstract class NotificationBuilder {

    /**
     * Create instance of an API-specific {@code NotificationBuilder} subclass.
     *
     * @param context
     *         A {@link Context} instance.
     *
     * @return Appropriate {@link NotificationBuilder} instance for this device.
     */
    public static NotificationBuilder createInstance(Context context) {
        Context appContext = context.getApplicationContext();

        NotificationBuilder instance;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            instance = new NotificationBuilderApi1(appContext);
        } else {
            instance = new NotificationBuilderApi11(appContext);
        }

        return instance;
    }


    protected Context mContext;

    /**
     * Constructor
     *
     * @param context
     *         A {@link Context} instance.
     */
    protected NotificationBuilder(Context context) {
        mContext = context;
    }

    /**
     * Set the small icon to use in the notification layouts.
     *
     * @param icon
     *         A resource ID in the application's package of the drawble to use.
     */
    public abstract void setSmallIcon(int icon);

    /**
     * Set the time that the event occurred.
     *
     * @param when
     *         Timestamp of when the event occurred.
     */
    public abstract void setWhen(long when);

    /**
     * Set the text that is displayed in the status bar when the notification first arrives.
     *
     * @param tickerText
     *         The text to display.
     */
    public abstract void setTicker(CharSequence tickerText);

    /**
     * Set the title (first row) of the notification, in a standard notification.
     *
     * @param title
     *         The text to display as notification title.
     */
    public abstract void setContentTitle(CharSequence title);

    /**
     * Set the text (second row) of the notification, in a standard notification.
     *
     * @param text
     *         The text to display.
     */
    public abstract void setContentText(CharSequence text);

    /**
     * Supply a PendingIntent to send when the notification is clicked.
     *
     * @param intent
     *         The intent that will be sent when the notification was clicked.
     */
    public abstract void setContentIntent(PendingIntent intent);

    /**
     * Set the large number at the right-hand side of the notification.
     *
     * @param number
     *         The number to display in the notification.
     */
    public abstract void setNumber(int number);

    /**
     * Set whether this is an ongoing notification.
     *
     * @param ongoing
     *         {@code true}, if it this is an ongoing notification. {@code false}, otherwise.
     */
    public abstract void setOngoing(boolean ongoing);

    /**
     * Setting this flag will make it so the notification is automatically canceled when the user
     * clicks it in the panel.
     *
     * @param autoCancel
     *         {@code true}, if the notification should be automatically cancelled when the user
     *         clicks on it. {@code false}, otherwise.
     */
    public abstract void setAutoCancel(boolean autoCancel);

    /**
     * Set the sound to play.
     *
     * It will play on the notification stream.
     *
     * @param sound
     *         The URI of the sound to play.
     */
    public abstract void setSound(Uri sound);

    /**
     * Set the vibration pattern to use.
     *
     * @param pattern
     *         An array of longs of times for which to turn the vibrator on or off.
     *
     * @see Vibrator#vibrate(long[], int)
     */
    public abstract void setVibrate(long[] pattern);

    /**
     * Set the color that you would like the LED on the device to blink, as well as the rate.
     *
     * @param argb
     *         The color the LED should blink.
     * @param onMs
     *         The number of milliseconds the LED should be on.
     * @param offMs
     *         The number of milliseconds the LED should be off.
     */
    public abstract void setLights(int argb, int onMs, int offMs);

    /**
     * Combine all of the options that have been set and return a new {@link Notification} object.
     *
     * @return A new {@code Notification} object configured by this {@link NotificationBuilder}.
     */
    public abstract Notification getNotification();
}
