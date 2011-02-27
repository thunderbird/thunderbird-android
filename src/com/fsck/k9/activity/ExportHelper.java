package com.fsck.k9.activity;

import android.app.Activity;
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
                                String toastText = activity.getString(R.string.settings_export_failure, message);
                                Toast toast = Toast.makeText(activity.getApplication(), toastText, Toast.LENGTH_LONG);
                                toast.show();
                            }
                        });
                    }

                    public void exportSuccess(final String fileName) {
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                progressable.setProgress(false);
                                String toastText = activity.getString(R.string.settings_export_success, fileName);
                                Toast toast = Toast.makeText(activity.getApplication(), toastText, Toast.LENGTH_LONG);
                                toast.show();
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

}
