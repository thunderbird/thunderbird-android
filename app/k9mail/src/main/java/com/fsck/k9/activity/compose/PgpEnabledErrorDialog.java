package com.fsck.k9.activity.compose;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.view.LayoutInflater;
import android.view.View;

import com.fsck.k9.R;
import com.fsck.k9.view.HighlightDialogFragment;


public class PgpEnabledErrorDialog extends HighlightDialogFragment {
    private static final String ARG_IS_GOTIT = "is_gotit";

    public static PgpEnabledErrorDialog newInstance(boolean isGotItDialog, @IdRes int showcaseView) {
        PgpEnabledErrorDialog dialog = new PgpEnabledErrorDialog();

        Bundle args = new Bundle();
        args.putInt(ARG_HIGHLIGHT_VIEW, showcaseView);
        args.putBoolean(ARG_IS_GOTIT, isGotItDialog);
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();

        boolean isGotItDialog = getArguments().getBoolean(ARG_IS_GOTIT);

        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(activity).inflate(R.layout.openpgp_enabled_error_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(view);

        builder.setNegativeButton(isGotItDialog ? R.string.openpgp_enabled_error_gotit :
                R.string.openpgp_enabled_error_back, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(R.string.openpgp_enabled_error_disable, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Activity activity = getActivity();
                if (activity == null) {
                    return;
                }

                ((OnOpenPgpDisableListener) activity).onOpenPgpClickDisable();
                dialog.dismiss();
            }
        });

        return builder.create();
    }

    public interface OnOpenPgpDisableListener {
        void onOpenPgpClickDisable();
    }
}
