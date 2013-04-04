package com.fsck.k9.helper;

import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

/**
 * Notification builder that will set {@link Notification#number} on pre-Honeycomb devices.
 *
 * @see <a href="http://code.google.com/p/android/issues/detail?id=38028">android - Issue 38028</a>
 */
public class NotificationBuilder extends NotificationCompat.Builder {
    protected int mNumber;


    public NotificationBuilder(Context context) {
        super(context);
    }

    @Override
    public NotificationCompat.Builder setNumber(int number) {
        super.setNumber(number);
        mNumber = number;
        return this;
    }

    @Override
    public Notification build() {
        Notification notification = super.build();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            notification.number = mNumber;
        }

        return notification;
    }
}
