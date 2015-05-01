package com.fsck.k9.activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.service.NotificationActionService;

public class NotificationDeleteConfirmation extends Activity {
    private final static String EXTRA_ACCOUNT = "account";
    private final static String EXTRA_MESSAGE_LIST = "messages";
    private final static String EXTRA_NOTIFICATION_ID = NotificationActionService.EXTRA_NOTIFICATION_ID;

    private final static int DIALOG_CONFIRM = 1;

    /**
     * The account to delete the messages on.
     */
    private Account mAccount;
    /**
     * The messages to delete.
     */
    private ArrayList<MessageReference> mMessageRefs;
    /**
     * ID of the notification that triggered this Activity.
     * To make sure we close the correte notification afterwards because
     * there may be multiple of them due to Android Wear stacked notifications.
     */
    private int mNotificationID;

    /**
     *
     * @param context context to create the PendingIntent.
     * @param account The account to delete the messages on.
     * @param refs The messages to delete.
     * @param notificationID ID of the notification that triggered this Activity.
     * @return PendingIntent that either deletes directly or shows a confirm-dialog on the phone (not on the wear device) first.
     */
    public static PendingIntent getIntent(final Context context, final Account account, final Serializable refs, final int notificationID) {
        Intent i = new Intent(context, NotificationDeleteConfirmation.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MESSAGE_LIST, refs);
        i.putExtra(EXTRA_NOTIFICATION_ID, notificationID);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return PendingIntent.getActivity(context, account.getAccountNumber(), i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setTheme(K9.getK9Theme() == K9.Theme.LIGHT ?
                R.style.Theme_K9_Dialog_Translucent_Light : R.style.Theme_K9_Dialog_Translucent_Dark);

        final Preferences preferences = Preferences.getPreferences(this);
        final Intent intent = getIntent();

        mAccount = preferences.getAccount(intent.getStringExtra(EXTRA_ACCOUNT));
        mMessageRefs = intent.getParcelableArrayListExtra(EXTRA_MESSAGE_LIST);
        mNotificationID = intent.getIntExtra(EXTRA_NOTIFICATION_ID, mAccount.getAccountNumber());

        if (mAccount == null || mMessageRefs == null || mMessageRefs.isEmpty()) {
            finish();
        } else if (!K9.confirmDeleteFromNotification()) {
            triggerDelete(mNotificationID);
            finish();
        } else {
            showDialog(DIALOG_CONFIRM);
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_CONFIRM:
            return ConfirmationDialog.create(this, id,
                    R.string.dialog_confirm_delete_title, "",
                    R.string.dialog_confirm_delete_confirm_button,
                    R.string.dialog_confirm_delete_cancel_button,
                    new Runnable() {
                        @Override
                        public void run() {
                            triggerDelete(mNotificationID);
                            finish();
                        }
                    },
                    new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    });
        }

        return super.onCreateDialog(id);
    }

    @Override
    public void onPrepareDialog(int id, Dialog d) {
        AlertDialog alert = (AlertDialog) d;
        switch (id) {
        case DIALOG_CONFIRM:
            int messageCount = mMessageRefs.size();
            alert.setMessage(getResources().getQuantityString(
                    R.plurals.dialog_confirm_delete_messages, messageCount, messageCount));
            break;
        }

        super.onPrepareDialog(id, d);
    }

    private void triggerDelete(final int notificationID) {
        Intent i = NotificationActionService.getDeleteAllMessagesIntent(this, mAccount, mMessageRefs, notificationID);
        startService(i);
    }
}
