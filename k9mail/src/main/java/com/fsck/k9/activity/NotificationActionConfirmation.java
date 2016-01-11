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


public class NotificationActionConfirmation extends Activity {
    private final static String EXTRA_ACTION = "action";
    private final static String EXTRA_ACCOUNT_UUID = "accountUuid";
    private final static String EXTRA_MESSAGE_REFERENCES = "messageReferences";

    private final static int DIALOG_CONFIRM = 1;

    private String action;
    private Account account;
    private ArrayList<MessageReference> messages;


    public static Intent getIntent(String action, Context context, MessageReference messageReference) {
        ArrayList<MessageReference> messageReferences = new ArrayList<MessageReference>(1);
        messageReferences.add(messageReference);

        return getIntent(action, context, messageReferences);
    }

    public static Intent getIntent(String action, Context context, ArrayList<MessageReference> messageReferences) {
        String accountUuid = messageReferences.get(0).getAccountUuid();

        Intent intent = new Intent(context, NotificationActionConfirmation.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_ACTION, action);
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
        String action = intent.getStringExtra(EXTRA_ACTION);
        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT_UUID);
        ArrayList<MessageReference> messages = intent.getParcelableArrayListExtra(EXTRA_MESSAGE_REFERENCES);

        if (action == null) {
            throw new IllegalArgumentException(EXTRA_ACTION + " can't be null");
        }

        if (!(action.equals(NotificationActionService.ACTION_DELETE) ||
              action.equals(NotificationActionService.ACTION_ARCHIVE) ||
              action.equals(NotificationActionService.ACTION_SPAM))) {
            throw new IllegalArgumentException("Invalid " + EXTRA_ACTION + ": " + action);
        }

        if (accountUuid == null) {
            throw new IllegalArgumentException(EXTRA_ACCOUNT_UUID + " can't be null");
        }

        if (messages == null) {
            throw new IllegalArgumentException(EXTRA_MESSAGE_REFERENCES + " can't be null");
        }

        if (messages.isEmpty()) {
            throw new IllegalArgumentException(EXTRA_MESSAGE_REFERENCES + " can't be empty");
        }

        Account account = getAccountFromUuid(accountUuid);
        if (account == null) {
            throw new IllegalStateException(EXTRA_ACCOUNT_UUID + " couldn't be resolved to an account");
        }

        this.action = action;
        this.account = account;
        this.messages = messages;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DIALOG_CONFIRM: {
                return createActionConfirmationDialog(dialogId);
            }
        }

        return super.onCreateDialog(dialogId);
    }

    @Override
    public void onPrepareDialog(int dialogId, @NonNull Dialog dialog) {
        AlertDialog alert = (AlertDialog) dialog;
        switch (dialogId) {
            case DIALOG_CONFIRM: {
                int messageCount = messages.size();
                int message =
                    action.equals(NotificationActionService.ACTION_DELETE) ?
                        R.plurals.dialog_confirm_delete_message :
                    action.equals(NotificationActionService.ACTION_ARCHIVE) ?
                        R.plurals.dialog_confirm_archive_message :
                    action.equals(NotificationActionService.ACTION_SPAM) ?
                        R.plurals.dialog_confirm_spam_message :
                    null;
                alert.setMessage(getResources().getQuantityString(
                    message, messageCount, messageCount));
                break;
            }
        }

        super.onPrepareDialog(dialogId, dialog);
    }

    private Account getAccountFromUuid(String accountUuid) {
        Preferences preferences = Preferences.getPreferences(this);
        return preferences.getAccount(accountUuid);
    }

    private Dialog createActionConfirmationDialog(int dialogId) {
        int message =
            action.equals(NotificationActionService.ACTION_DELETE) ?
                R.plurals.dialog_confirm_delete_message :
            action.equals(NotificationActionService.ACTION_ARCHIVE) ?
                R.plurals.dialog_confirm_archive_message :
            action.equals(NotificationActionService.ACTION_SPAM) ?
                R.plurals.dialog_confirm_spam_message :
            null;
        int confirm_button =
            action.equals(NotificationActionService.ACTION_DELETE) ?
                R.string.dialog_confirm_delete_confirm_button :
            action.equals(NotificationActionService.ACTION_ARCHIVE) ?
                R.string.dialog_confirm_archive_confirm_button :
            action.equals(NotificationActionService.ACTION_SPAM) ?
                R.string.dialog_confirm_spam_confirm_button :
            null;
        int cancel_button =
            action.equals(NotificationActionService.ACTION_DELETE) ?
                R.string.dialog_confirm_delete_cancel_button :
            action.equals(NotificationActionService.ACTION_ARCHIVE) ?
                R.string.dialog_confirm_archive_cancel_button :
            action.equals(NotificationActionService.ACTION_SPAM) ?
                R.string.dialog_confirm_spam_cancel_button :
            null;

        return ConfirmationDialog.create(this, dialogId, message, "", confirm_button, cancel_button,
                new Runnable() {
                    @Override
                    public void run() {
                        actionAndFinish();
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
    }

    private void actionAndFinish() {
        cancelNotifications();
        triggerAction();
        finish();
    }

    private void cancelNotifications() {
        MessagingController controller = MessagingController.getInstance(this);
        for (MessageReference messageReference : messages) {
            controller.cancelNotificationForMessage(account, messageReference);
        }
    }

    private void triggerAction() {
        String accountUuid = account.getUuid();
        Intent intent = NotificationActionService.createActionAllMessagesIntent(action, this, accountUuid, messages);
        startService(intent);
    }
}
