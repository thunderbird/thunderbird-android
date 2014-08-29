package com.fsck.k9.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;

import com.fsck.k9.K9;

public class ConfirmationDialogFragment extends DialogFragment implements OnClickListener,
        OnCancelListener {
    private ConfirmationDialogFragmentListener mListener;

    private static final String ARG_DIALOG_ID = "dialog_id";
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_CONFIRM_TEXT = "confirm";
    private static final String ARG_CANCEL_TEXT = "cancel";


    public static ConfirmationDialogFragment newInstance(int dialogId, String title, String message,
            String confirmText, String cancelText) {
        ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_DIALOG_ID, dialogId);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_CONFIRM_TEXT, confirmText);
        args.putString(ARG_CANCEL_TEXT, cancelText);
        fragment.setArguments(args);

        return fragment;
    }

    public static ConfirmationDialogFragment newInstance(int dialogId, String title, String message,
            String cancelText) {
        return newInstance(dialogId, title, message, null, cancelText);
    }


    public interface ConfirmationDialogFragmentListener {
        void doPositiveClick(int dialogId);
        void doNegativeClick(int dialogId);
        void dialogCancelled(int dialogId);
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        String title = args.getString(ARG_TITLE);
        String message = args.getString(ARG_MESSAGE);
        String confirmText = args.getString(ARG_CONFIRM_TEXT);
        String cancelText = args.getString(ARG_CANCEL_TEXT);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        builder.setMessage(message);
        if (confirmText != null && cancelText != null) {
            builder.setPositiveButton(confirmText, this);
            builder.setNegativeButton(cancelText, this);
        } else if (cancelText != null) {
            builder.setNeutralButton(cancelText, this);
        } else {
            throw new RuntimeException("Set at least cancelText!");
        }

        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE: {
                getListener().doPositiveClick(getDialogId());
                break;
            }
            case DialogInterface.BUTTON_NEGATIVE: {
                getListener().doNegativeClick(getDialogId());
                break;
            }
            case DialogInterface.BUTTON_NEUTRAL: {
                getListener().doNegativeClick(getDialogId());
                break;
            }
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        getListener().dialogCancelled(getDialogId());
    }

    private int getDialogId() {
        return getArguments().getInt(ARG_DIALOG_ID);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ConfirmationDialogFragmentListener) activity;
        } catch (ClassCastException e) {
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, activity.toString() + " did not implement ConfirmationDialogFragmentListener");
        }
    }

    private ConfirmationDialogFragmentListener getListener() {
        if (mListener != null) {
            return mListener;
        }

        // fallback to getTargetFragment...
        try {
            return (ConfirmationDialogFragmentListener) getTargetFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(getTargetFragment().getClass() +
                    " must implement ConfirmationDialogFragmentListener");
        }
    }
}
