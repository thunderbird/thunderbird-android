package com.fsck.k9.fragment;


import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;

import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.SimpleMessagingListener;


public class AttachmentDownloadDialogFragment extends DialogFragment {
    private static final String ARG_SIZE = "size";
    private static final String ARG_MESSAGE = "message";


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

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        long size = args.getLong(ARG_SIZE);
        String message = args.getString(ARG_MESSAGE);

        final SizeUnit sizeUnit = SizeUnit.getAppropriateFor(size);

        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(message);
        dialog.setMax(sizeUnit.valueInSizeUnit(size));
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setProgress(0);
        dialog.setProgressNumberFormat("%1d/%2d " + sizeUnit.shortName);
        dialog.show();

        messagingListener = new SimpleMessagingListener() {
            @Override
            public void updateProgress(int progress) {
                dialog.setProgress(sizeUnit.valueInSizeUnit(progress));
            }
        };

        messagingController = MessagingController.getInstance(getActivity());
        messagingController.addListener(messagingListener);

        return dialog;
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


    private enum SizeUnit {
        BYTE("B", 1L),
        KIBIBYTE("KiB", 1024L),
        MEBIBYTE("MiB", 1024L * 1024L),
        GIBIBYTE("GiB", 1024L * 1024L * 1024L),
        TEBIBYTE("TiB", 1024L * 1024L * 1024L * 1024L),
        PEBIBYTE("PiB", 1024L * 1024L * 1024L * 1024L * 1024L);

        public final String shortName;
        public final long size;


        static SizeUnit getAppropriateFor(long value) {
            for (SizeUnit sizeUnit : values()) {
                if (value < 1024L * 10L * sizeUnit.size) {
                    return sizeUnit;
                }
            }
            return SizeUnit.BYTE;
        }


        SizeUnit(String shortName, long size) {
            this.shortName = shortName;
            this.size = size;
        }

        int valueInSizeUnit(long value) {
            return (int) (value / size);
        }
    }

    public interface AttachmentDownloadCancelListener {
        void onProgressCancel(AttachmentDownloadDialogFragment fragment);
    }
}
