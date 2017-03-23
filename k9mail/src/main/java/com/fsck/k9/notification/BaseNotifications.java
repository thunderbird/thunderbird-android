package com.fsck.k9.notification;


import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.Builder;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationQuickDelete;
import com.fsck.k9.R;
import com.fsck.k9.helper.ContactPicture;
import com.fsck.k9.helper.Contacts;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


abstract class BaseNotifications {
    protected final Context context;
    protected final NotificationController controller;
    protected final NotificationActionCreator actionCreator;


    protected BaseNotifications(NotificationController controller, NotificationActionCreator actionCreator) {
        this.context = controller.getContext();
        this.controller = controller;
        this.actionCreator = actionCreator;
    }

    protected NotificationCompat.Builder createBigTextStyleNotification(Account account, NotificationHolder holder,
            int notificationId) {
        String accountName = controller.getAccountName(account);
        NotificationContent content = holder.content;
        String groupKey = NotificationGroupKeys.getGroupKey(account);

        Uri photoUri = Contacts.getInstance(context).getPhotoUri(content.from.getAddress());
        Bitmap bitmap = null;
        if (photoUri != null) {
            try {
                InputStream stream = context.getContentResolver().openInputStream(photoUri);
                if (stream != null) {
                    try {
                        Bitmap tempBitmap = BitmapFactory.decodeStream(stream);
                        if (tempBitmap != null) {
                            int largeIconWidth = Resources.getSystem()
                                    .getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
                            int largeIconHeight = Resources.getSystem()
                                    .getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
                            bitmap = Bitmap.createScaledBitmap(tempBitmap, largeIconWidth, largeIconHeight, true);
                            if (tempBitmap != bitmap) {
                                tempBitmap.recycle();
                            }
                        }
                    } finally {
                        try {
                            stream.close();
                        } catch (IOException e) { /* ignore */ }
                    }
                }
            } catch (FileNotFoundException e) {
                    /* ignore */
            }
        }
        if (bitmap == null) {
            bitmap = ContactPicture.getContactPictureLoader(context).getContactPicture(content.from);
        }

        NotificationCompat.Builder builder = createAndInitializeNotificationBuilder(account)
                .setTicker(content.summary)
                .setGroup(groupKey)
                .setLargeIcon(bitmap)
                .setContentTitle(content.sender)
                .setContentText(content.subject)
                .setSubText(accountName);

        NotificationCompat.BigTextStyle style = createBigTextStyle(builder);
        style.bigText(content.preview);

        builder.setStyle(style);

        PendingIntent contentIntent = actionCreator.createViewMessagePendingIntent(
                content.messageReference, notificationId);
        builder.setContentIntent(contentIntent);

        return builder;
    }

    protected NotificationCompat.Builder createAndInitializeNotificationBuilder(Account account) {
        return controller.createNotificationBuilder()
                .setSmallIcon(getNewMailNotificationIcon())
                .setColor(account.getChipColor())
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_EMAIL);
    }

    protected boolean isDeleteActionEnabled() {
        NotificationQuickDelete deleteOption = K9.getNotificationQuickDeleteBehaviour();
        return deleteOption == NotificationQuickDelete.ALWAYS || deleteOption == NotificationQuickDelete.FOR_SINGLE_MSG;
    }

    protected BigTextStyle createBigTextStyle(Builder builder) {
        return new BigTextStyle(builder);
    }

    private int getNewMailNotificationIcon() {
        return R.drawable.notification_icon_new_mail;
    }
}
