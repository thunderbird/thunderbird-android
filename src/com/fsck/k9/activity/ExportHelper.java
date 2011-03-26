package com.fsck.k9.activity;

import java.util.HashSet;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;

import com.fsck.k9.R;
import com.fsck.k9.preferences.StorageFormat;

public class ExportHelper {
    public static void exportSettings(final Activity activity, final HashSet<String> accountUuids, final ExportListener listener) {
        // Once there are more versions, build a UI to select which one to use.  For now, use the encrypted/encoded version:
        String version = StorageFormat.ENCRYPTED_XML_FILE;
        AsyncUIProcessor.getInstance(activity.getApplication()).exportSettings(activity, version, accountUuids, new ExportListener() {

            @Override
            public void canceled() {
                if (listener != null) {
                    listener.canceled();
                }
            }

            @Override
            public void failure(String message, Exception e) {
                if (listener != null) {
                    listener.failure(message, e);
                }
                showDialog(activity, R.string.settings_export_failed_header, activity.getString(R.string.settings_export_failure, message));
            }

            @Override
            public void started() {
                if (listener != null) {
                    listener.started();
                }
                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        String toastText = activity.getString(R.string.settings_exporting);
                        Toast toast = Toast.makeText(activity, toastText, Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });
            }

            @Override
            public void success(String fileName) {
                if (listener != null) {
                    listener.success(fileName);
                }
                showDialog(activity, R.string.settings_export_success_header, activity.getString(R.string.settings_export_success, fileName));
            }

            @Override
            public void success() {
                // This one should never be called here because the AsyncUIProcessor will generate a filename
            }
        });
    }

    private static void showDialog(final Activity activity, final int headerRes, final String message) {
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
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
        });

    }
}
