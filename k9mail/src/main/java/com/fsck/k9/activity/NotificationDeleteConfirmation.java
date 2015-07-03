package com.fsck.k9.activity;

import java.io.Serializable;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.notification.NotificationActionService;

public class NotificationDeleteConfirmation extends Activity {
    private final static String EXTRA_ACCOUNT_UUID = "accountUuid";
    private final static String EXTRA_MESSAGE_LIST = "messages";
    private final static String EXTRA_NOTIFICATION_ID = NotificationActionService.EXTRA_NOTIFICATION_ID;

    private final static int DIALOG_CONFIRM = 1;
    private static final int INVALID_NOTIFICATION_ID = -1;


    private Account account;
    private ArrayList<MessageReference> messagesToDelete;
    private int notificationId;


    public static PendingIntent getIntent(Context context, Account account, Serializable messageToDelete,
            int notificationId) {
        Intent intent = new Intent(context, NotificationDeleteConfirmation.class);
        intent.putExtra(EXTRA_ACCOUNT_UUID, account.getUuid());
        intent.putExtra(EXTRA_MESSAGE_LIST, messageToDelete);
        intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int accountNumber = account.getAccountNumber();
        return PendingIntent.getActivity(context, accountNumber, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setTheme(K9.getK9Theme() == K9.Theme.LIGHT ?
                R.style.Theme_K9_Dialog_Translucent_Light : R.style.Theme_K9_Dialog_Translucent_Dark);

        Intent intent = getIntent();
        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT_UUID);

        account = getAccountFromUuid(accountUuid);
        messagesToDelete = intent.getParcelableArrayListExtra(EXTRA_MESSAGE_LIST);
        notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, INVALID_NOTIFICATION_ID);

        if (account == null || messagesToDelete == null || messagesToDelete.isEmpty() ||
                notificationId == INVALID_NOTIFICATION_ID) {
            finish();
        } else if (!K9.confirmDeleteFromNotification()) {
            triggerDelete(notificationId);
            finish();
        } else {
            showDialog(DIALOG_CONFIRM);
        }
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DIALOG_CONFIRM: {
                return createDeleteConfirmationDialog(dialogId);
            }
        }

        return super.onCreateDialog(dialogId);
    }

    @Override
    public void onPrepareDialog(int dialogId, @NonNull Dialog dialog) {
        AlertDialog alert = (AlertDialog) dialog;
        switch (dialogId) {
            case DIALOG_CONFIRM: {
                int messageCount = messagesToDelete.size();
                alert.setMessage(getResources().getQuantityString(
                        R.plurals.dialog_confirm_delete_messages, messageCount, messageCount));
                break;
            }
        }

        super.onPrepareDialog(dialogId, dialog);
    }

    private Dialog createDeleteConfirmationDialog(int dialogId) {
        return ConfirmationDialog.create(this, dialogId,
                R.string.dialog_confirm_delete_title, "",
                R.string.dialog_confirm_delete_confirm_button,
                R.string.dialog_confirm_delete_cancel_button,
                new Runnable() {
                    @Override
                    public void run() {
                        triggerDelete(notificationId);
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

    private Account getAccountFromUuid(String accountUuid) {
        Preferences preferences = Preferences.getPreferences(this);
        return preferences.getAccount(accountUuid);
    }

    private void triggerDelete(int notificationId) {
        Intent intent = NotificationActionService.createDeletePendingIntent(
                this, account, messagesToDelete, notificationId);

        startService(intent);
    }
}
