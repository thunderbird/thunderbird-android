package com.fsck.k9.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;


public class ProgressDialogFragment extends DialogFragment {
    protected static final String ARG_TITLE = "title";
    protected static final String ARG_MESSAGE = "message";

    public static ProgressDialogFragment newInstance(String title, String message) {
        ProgressDialogFragment fragment = new ProgressDialogFragment();

        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);

        return fragment;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        String title = args.getString(ARG_TITLE);
        String message = args.getString(ARG_MESSAGE);

        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setIndeterminate(true);
        dialog.setTitle(title);
        dialog.setMessage(message);

        return dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        Activity activity = getActivity();
        if (activity != null && activity instanceof CancelListener) {
            CancelListener listener = (CancelListener) activity;
            listener.onProgressCancel(this);
        }

        super.onCancel(dialog);
    }

    public interface CancelListener {
        void onProgressCancel(ProgressDialogFragment fragment);
    }
}
