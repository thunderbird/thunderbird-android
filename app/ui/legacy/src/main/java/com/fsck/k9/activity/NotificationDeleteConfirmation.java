package com.fsck.k9.activity;


import java.util.Collections;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.MessageReference;
import com.fsck.k9.ui.R;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.notification.NotificationActionService;
import com.fsck.k9.ui.base.K9ActivityCommon;
import com.fsck.k9.ui.base.ThemeType;

import static com.fsck.k9.controller.MessageReferenceHelper.toMessageReferenceList;
import static com.fsck.k9.controller.MessageReferenceHelper.toMessageReferenceStringList;


public class NotificationDeleteConfirmation extends AppCompatActivity {
    private final static String EXTRA_ACCOUNT_UUID = "accountUuid";
    private final static String EXTRA_MESSAGE_REFERENCES = "messageReferences";

    private final static int DIALOG_CONFIRM = 1;


    private final K9ActivityCommon base = new K9ActivityCommon(this, ThemeType.DIALOG);

    private Account account;
    private List<MessageReference> messagesToDelete;


    public static Intent getIntent(Context context, MessageReference messageReference) {
        return getIntent(context, Collections.singletonList(messageReference));
    }

    public static Intent getIntent(Context context, List<MessageReference> messageReferences) {
        String accountUuid = messageReferences.get(0).getAccountUuid();

        Intent intent = new Intent(context, NotificationDeleteConfirmation.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_ACCOUNT_UUID, accountUuid);
        intent.putExtra(EXTRA_MESSAGE_REFERENCES, toMessageReferenceStringList(messageReferences));

        return intent;
    }

    @Override
    public void onCreate(Bundle icicle) {
        base.preOnCreate();
        super.onCreate(icicle);

        extractExtras();

        showDialog(DIALOG_CONFIRM);
    }

    @Override
    protected void onResume() {
        base.preOnResume();
        super.onResume();
    }

    private void extractExtras() {
        Intent intent = getIntent();
        String accountUuid = intent.getStringExtra(EXTRA_ACCOUNT_UUID);
        List<String> messageReferenceStrings = intent.getStringArrayListExtra(EXTRA_MESSAGE_REFERENCES);
        List<MessageReference> messagesToDelete = toMessageReferenceList(messageReferenceStrings);

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
