package com.fsck.k9.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.R;

public class ExportHelper {
    public static void exportSettings(final Activity activity, final Progressable progressable, final Account account) {
        PasswordEntryDialog dialog = new PasswordEntryDialog(activity, activity.getString(R.string.settings_encryption_password_prompt),
        new PasswordEntryDialog.PasswordEntryListener() {
            public void passwordChosen(String chosenPassword) {
                String toastText = activity.getString(R.string.settings_exporting);
                Toast toast = Toast.makeText(activity, toastText, Toast.LENGTH_SHORT);
                toast.show();
                progressable.setProgress(true);
                String uuid = null;
                if (account != null) {
                    uuid = account.getUuid();
                }
                AsyncUIProcessor.getInstance(activity.getApplication()).exportSettings(uuid, chosenPassword,
                new ExportListener() {
                    public void failure(final String message, Exception e) {
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                progressable.setProgress(false);
                                showDialog(activity, R.string.settings_export_failed_header, activity.getString(R.string.settings_export_failure, message));
                            }
                        });
                    }

                    public void exportSuccess(final String fileName) {
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                progressable.setProgress(false);
                                showDialog(activity, R.string.settings_export_success_header, activity.getString(R.string.settings_export_success, fileName));
                            }
                        });
                    }
                });
            }

            public void cancel() {
            }
        });
        dialog.show();
    }
    private static void showDialog(final Activity activity, int headerRes, String message)
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(headerRes);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.okay_action,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
       
        builder.show();
    }
}
