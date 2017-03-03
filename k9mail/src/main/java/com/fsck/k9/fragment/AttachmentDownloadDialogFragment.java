package com.fsck.k9.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.SimpleMessagingListener;

public class AttachmentDownloadDialogFragment extends DialogFragment {

    static ProgressDialog dialog;

    static int progress = 0;

    static MessagingListener messagingListener;

    protected static final String ARG_SIZE = "size";
    protected static final String ARG_MESSAGE = "message";

    public static AttachmentDownloadDialogFragment newInstance(int size, String message) {
        AttachmentDownloadDialogFragment attachmentDownloadDialogFragment = new AttachmentDownloadDialogFragment();

        progress = 0;

        Bundle args = new Bundle();
        args.putInt(ARG_SIZE, size);
        args.putString(ARG_MESSAGE, message);
        attachmentDownloadDialogFragment.setArguments(args);

        return attachmentDownloadDialogFragment;
    }

    public void updateDownloadProgress(int newProgress) {
        progress = newProgress;
        for (int i = progress; i <= newProgress; i++) {
            dialog.setProgress(i);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        int size = args.getInt(ARG_SIZE);
        String message = args.getString(ARG_MESSAGE);

        messagingListener = new SimpleMessagingListener(){
            @Override
            public void updateProgress(int progress) {
                updateDownloadProgress(progress);
            }
        };

        MessagingController.getInstance(getActivity()).addDownloadProgressListener(messagingListener);

        dialog = new ProgressDialog(getActivity());
        dialog.setMessage(message);
        dialog.setMax(size);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setProgress(0);
        dialog.show();

        return dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        Activity activity = getActivity();
        if (activity != null && activity instanceof AttachmentDownloadCancelListener) {
            AttachmentDownloadCancelListener listener = (AttachmentDownloadCancelListener) activity;
            listener.onProgressCancel(this);
        }

        super.onCancel(dialog);
    }

    public interface AttachmentDownloadCancelListener {
        void onProgressCancel(AttachmentDownloadDialogFragment fragment);
    }

}
