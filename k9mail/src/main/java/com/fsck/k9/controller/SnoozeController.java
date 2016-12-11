package com.fsck.k9.controller;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.activity.ChooseFolder;
import com.fsck.k9.activity.ChooseSnooze;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.notification.NotificationPublisher;

/**
 * Manages snoozing messages until later.
 *
 * Created by odbol on 12/10/16.
 */
public class SnoozeController {


    private final Context context;
    private int curIntentId = 0;

    public SnoozeController(Context c) {
        context = c;
    }


    /***
     * Launches an activity to choose the snooze time. Must also call #handleActivityResult() from
     * your activity's onActivityResult() in order to deal with the result.
     *
     */
    public void launchSnoozeDialog(Fragment frag, Account account, MessageReference message, int resultCode) {
        Intent intent = new Intent(frag.getActivity(), ChooseSnooze.class);
        intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, account.getUuid());
        intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, message.getFolderName());
        intent.putExtra(ChooseFolder.EXTRA_MESSAGE, message);
        frag.startActivityForResult(intent, resultCode);
    }

    /***
     * Call from your activity's onActivityResult().
     *
     * @param resultCode
     * @param data
     * @return The message that was snoozed, or null on error/no result.
     */
    public MessageReference handleActivityResult(int resultCode, Intent data) {

        if (data == null || resultCode != Activity.RESULT_OK) {
            return null;
        }

        MessageReference ref = data.getParcelableExtra(ChooseFolder.EXTRA_MESSAGE);
        long snoozeUntil = data.getLongExtra(ChooseSnooze.EXTRA_SNOOZE_UNTIL, 0L);
        if (snoozeUntil <= 0) return null;

        snoozeMessage(ref, snoozeUntil);

        return ref;
    }


    /***
     * Snooze a message until the given time.
     *
     * @param msg the message to snooze
     * @param snoozeUntil the timestamp to notify the user, in milliseconds.
     */
    public void snoozeMessage(MessageReference msg, long snoozeUntil) {

        Resources res = context.getResources();
        String txt = String.format(
                res.getString(R.string.will_remind_you),
                getSnoozeMessage(snoozeUntil)
        );

        scheduleNotification(msg, snoozeUntil);

        Toast.makeText(context, txt, Toast.LENGTH_SHORT)
                .show();
    }

    public static CharSequence getSnoozeMessage(long timestamp) {
        return DateUtils.getRelativeTimeSpanString(
                timestamp + 61000, // add a minute, so it doesn't say "in 59 minutes" etc.
                System.currentTimeMillis(),
                0, //DateUtils.DAY_IN_MILLIS,
                0);
    }


    private void scheduleNotification(MessageReference msg, long timestamp) {
        Intent notificationIntent = new Intent(context, NotificationPublisher.class);
        notificationIntent.putExtra(NotificationPublisher.EXTRA_MESSAGE, msg);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                curIntentId++,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, timestamp, pendingIntent);
    }
}
