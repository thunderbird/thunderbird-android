package com.fsck.k9.ui.dialog;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.fsck.k9.R;


public class ApgDeprecationWarningDialog extends AlertDialog {
    public ApgDeprecationWarningDialog(Context context) {
        super(context);

        LayoutInflater inflater = LayoutInflater.from(context);

        @SuppressLint("InflateParams")
        View contentView = inflater.inflate(R.layout.dialog_apg_deprecated, null);

        TextView textViewLearnMore = (TextView) contentView.findViewById(R.id.apg_learn_more);
        makeTextViewLinksClickable(textViewLearnMore);

        setIcon(R.drawable.ic_apg_small);
        setTitle(R.string.apg_deprecated_title);
        setView(contentView);
        setButton(Dialog.BUTTON_NEUTRAL, context.getString(R.string.apg_deprecated_ok), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                cancel();
            }
        });
    }

    private void makeTextViewLinksClickable(TextView textView) {
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
