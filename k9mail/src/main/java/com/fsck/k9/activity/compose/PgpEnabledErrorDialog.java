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
    public static PgpEnabledErrorDialog newInstance(@IdRes int showcaseView) {
        PgpEnabledErrorDialog dialog = new PgpEnabledErrorDialog();

        Bundle args = new Bundle();
        args.putInt(ARG_HIGHLIGHT_VIEW, showcaseView);
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();

        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(activity).inflate(R.layout.openpgp_enabled_error_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(view);

        builder.setNegativeButton("Back", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("Disable Encryption", new OnClickListener() {
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
