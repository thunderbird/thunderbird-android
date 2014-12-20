package com.fsck.k9.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;

public class ConfirmationDialog {

    /**
     * Creates a customized confirmation dialog ({@link AlertDialog}).
     *
     * @param activity The activity this dialog is created for.
     * @param dialogId The id that was used with {@link Activity#showDialog(int)}
     * @param title The resource id of the text to display in the dialog title
     * @param message The text to display in the main dialog area
     * @param confirmButton The resource id of the text to display in the confirm button
     * @param cancelButton The resource id of the text to display in the cancel button
     * @param action The action to perform if the user presses the confirm button
     * @return A confirmation dialog with the supplied arguments
     */
    public static Dialog create(final Activity activity, final int dialogId, final int title,
                                final String message, final int confirmButton, final int cancelButton,
                                final Runnable action) {
        return create(activity, dialogId, title, message, confirmButton, cancelButton,
                      action, null);
    }

    /**
     * Creates a customized confirmation dialog ({@link AlertDialog}).
     *
     * @param activity The activity this dialog is created for.
     * @param dialogId The id that was used with {@link Activity#showDialog(int)}
     * @param title The resource id of the text to display in the dialog title
     * @param message The text to display in the main dialog area
     * @param confirmButton The resource id of the text to display in the confirm button
     * @param cancelButton The resource id of the text to display in the cancel button
     * @param action The action to perform if the user presses the confirm button
     * @param negativeAction The action to perform if the user presses the cancel button. Can be {@code null}.
     * @return A confirmation dialog with the supplied arguments
     */
    public static Dialog create(final Activity activity, final int dialogId, final int title,
                                final String message, final int confirmButton, final int cancelButton,
                                final Runnable action, final Runnable negativeAction) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(confirmButton,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                activity.dismissDialog(dialogId);
                action.run();
            }
        });
        builder.setNegativeButton(cancelButton,
        new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                activity.dismissDialog(dialogId);
                if (negativeAction != null) {
                    negativeAction.run();
                }
            }
        });
        return builder.create();
    }

    /**
     * Creates a customized confirmation dialog ({@link AlertDialog}).
     *
     * @param activity The activity this dialog is created for.
     * @param dialogId The id that was used with {@link Activity#showDialog(int)}
     * @param title The resource id of the text to display in the dialog title
     * @param message The resource id of text to display in the main dialog area
     * @param confirmButton The resource id of the text to display in the confirm button
     * @param cancelButton The resource id of the text to display in the cancel button
     * @param action The action to perform if the user presses the confirm button
     * @return A confirmation dialog with the supplied arguments
     * @see #create(Activity,int,int,String,int,int,Runnable, Runnable)
     */
    public static Dialog create(final Activity activity, final int dialogId, final int title,
                                final int message, final int confirmButton, final int cancelButton,
                                final Runnable action) {

        return create(activity, dialogId, title, activity.getString(message), confirmButton,
                      cancelButton, action, null);
    }
}
