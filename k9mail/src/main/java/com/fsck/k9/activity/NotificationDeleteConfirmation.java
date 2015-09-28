package com.fsck.k9.activity;


import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.notification.NotificationActionService;


public class NotificationDeleteConfirmation extends Activity {
    private final static String EXTRA_ACCOUNT_UUID = "accountUuid";
    private final static String EXTRA_MESSAGE_REFERENCES = "messageReferences";

    private final static int DIALOG_CONFIRM = 1;

    private Account account;
    private ArrayList<MessageReference> messagesToDelete;


    public static Intent getIntent(Context context, MessageReference messageReference) {
        ArrayList<MessageReference> messageReferences = new ArrayList<MessageReference>(1);
        messageReferences.add(messageReference);

        return getIntent(context, messageReferences);
    }

    public static Intent getIntent(Context context, ArrayList<MessageReference> messageReferences) {
        String accountUuid = messageReferences.get(0).getAccountUuid();

        Intent intent = new Intent(context, NotificationDeleteConfirmation.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_ACCOUNT_UUID, accountUuid);
        intent.putExtra(EXTRA_MESSAGE_REFERENCES, messageReferences);

        return intent;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setTheme(K9.getK9Theme() == K9.Theme.LIGHT ?
                R.style.Theme_K9_Dialog_Translucent_Light : R.style.Theme_K9_Dialog_Translucent_Dark);

        extractExtras();

        showDialog(DIALOG_CONFIRM);
    }

    private void extractExtras() {
        Intent intent = getIntent();
        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT_UUID);
        ArrayList<MessageReference> messagesToDelete = intent.getParcelableArrayListExtra(EXTRA_MESSAGE_REFERENCES);

        if (accountUuid == null) {
            throw new IllegalArgumentException(EXTRA_ACCOUNT_UUID + " can't be null");
        }

        if (messagesToDelete == null) {
            throw new IllegalArgumentException(EXTRA_MESSAGE_REFERENCES + " can't be null");
        }

        if (messagesToDelete.isEmpty()) {
            throw new IllegalArgumentException(EXTRA_MESSAGE_REFERENCES + " can't be empty");
        }

        Account account = getAccountFromUuid(accountUuid);
        if (account == null) {
            throw new IllegalStateException(EXTRA_ACCOUNT_UUID + " couldn't be resolved to an account");
        }

        this.account = account;
        this.messagesToDelete = messagesToDelete;
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

    private Account getAccountFromUuid(String accountUuid) {
        Preferences preferences = Preferences.getPreferences(this);
        return preferences.getAccount(accountUuid);
    }

    private Dialog createDeleteConfirmationDialog(int dialogId) {
        return ConfirmationDialog.create(this, dialogId,
                R.string.dialog_confirm_delete_title, "",
                R.string.dialog_confirm_delete_confirm_button,
                R.string.dialog_confirm_delete_cancel_button,
                new Runnable() {
                    @Override
                    public void run() {
                        deleteAndFinish();
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
    }

    private void deleteAndFinish() {
        cancelNotifications();
        triggerDelete();
        finish();
    }

    private void cancelNotifications() {
        MessagingController controller = MessagingController.getInstance(this);
        for (MessageReference messageReference : messagesToDelete) {
            controller.cancelNotificationForMessage(account, messageReference);
        }
    }

    private void triggerDelete() {
        String accountUuid = account.getUuid();
        Intent intent = NotificationActionService.createDeleteAllMessagesIntent(this, accountUuid, messagesToDelete);
        startService(intent);
    }
}
