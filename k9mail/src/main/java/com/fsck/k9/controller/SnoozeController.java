package com.fsck.k9.controller;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.activity.ChooseFolder;
import com.fsck.k9.activity.ChooseSnooze;
import com.fsck.k9.activity.MessageReference;

/**
 * Manages snoozing messages until later.
 *
 * Created by odbol on 12/10/16.
 */
public class SnoozeController {


    private final Context context;

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

        String txt = "Will remind you " + getSnoozeMessage(snoozeUntil);

        Toast.makeText(context, txt, Toast.LENGTH_SHORT)
                .show();
    }

    public static CharSequence getSnoozeMessage(long timestamp) {
        return DateUtils.getRelativeTimeSpanString(
                timestamp,
                System.currentTimeMillis(),
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE);
    }
}
