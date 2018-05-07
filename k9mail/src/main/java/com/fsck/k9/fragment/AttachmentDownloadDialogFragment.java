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
    private static final String ARG_SIZE = "size";
    private static final String ARG_MESSAGE = "message";


    private ProgressDialog dialog;
    private MessagingListener messagingListener;
    private MessagingController messagingController;


    public static AttachmentDownloadDialogFragment newInstance(long size, String message) {
        AttachmentDownloadDialogFragment fragment = new AttachmentDownloadDialogFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_SIZE, size);
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);

        return fragment;
    }


    private SizeUnit getAppropriateSizeUnit(long size) {
        for (SizeUnit sizeUnit : SizeUnit.values()) {
            if (size < 1024 * 10 * sizeUnit.size) {
                return sizeUnit;
            }
        }
        return SizeUnit.B;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        long size = args.getLong(ARG_SIZE);
        String message = args.getString(ARG_MESSAGE);

        final SizeUnit sizeUnit = getAppropriateSizeUnit(size);


        messagingListener = new SimpleMessagingListener() {
            @Override
            public void updateProgress(int progress) {
                dialog.setProgress((int) (progress / sizeUnit.size));
            }
        };

        messagingController = MessagingController.getInstance(getActivity());
        messagingController.addListener(messagingListener);

        dialog = new ProgressDialog(getActivity());
        dialog.setMessage(message);
        dialog.setMax((int) (size / sizeUnit.size));
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setProgress(0);
        dialog.setProgressNumberFormat("%1d/%2d " + sizeUnit.name());
        dialog.show();

        return dialog;
    }

    private enum SizeUnit {
        B(1), KB(1024L), MB(1024L * 1024L), GB(1024L * 1024L * 1024L), TB(1024L * 1024L * 1024L * 1024L), PB(
                1024L * 1024L * 1024L * 1024L * 1024L);

        public final long size;

        SizeUnit(long size) {
            this.size = size;
        }
    }

    @Override
    public void onDestroyView() {
        messagingController.removeListener(messagingListener);
        super.onDestroyView();
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
