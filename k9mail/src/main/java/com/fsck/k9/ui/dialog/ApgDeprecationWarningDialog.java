package com.fsck.k9.ui.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.fsck.k9.R;


public class ApgDeprecationWarningDialog extends DialogFragment {

    private OnDismissListener onDismissListener;

    public static ApgDeprecationWarningDialog newInstance() {
        return new ApgDeprecationWarningDialog();
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        if (context == null) {
            return null;
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        View contentView = inflater.inflate(R.layout.dialog_apg_deprecated, null, false);

        TextView textViewLearnMore = (TextView) contentView.findViewById(R.id.apg_learn_more);
        makeTextViewLinksClickable(textViewLearnMore);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setIcon(R.drawable.ic_apg_small);
        builder.setTitle(R.string.apg_deprecated_title);
        builder.setView(contentView);
        builder.setNeutralButton(R.string.apg_deprecated_ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dismiss();
            }
        });
        builder.setCancelable(false);

        return builder.create();
    }

    private void makeTextViewLinksClickable(TextView textView) {
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void setOnDismissListener(OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (onDismissListener != null) {
            onDismissListener.onDismiss(dialog);
        }
    }
}
