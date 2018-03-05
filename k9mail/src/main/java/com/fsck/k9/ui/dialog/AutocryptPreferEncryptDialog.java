package com.fsck.k9.ui.dialog;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.TextView;

import com.fsck.k9.R;


public class AutocryptPreferEncryptDialog extends AlertDialog implements OnClickListener {

    private final CheckBox preferEncryptCheckbox;
    private final OnPreferEncryptChangedListener onPreferEncryptChangedListener;

    private boolean preferEncryptEnabled;

    public AutocryptPreferEncryptDialog(Context context, boolean preferEncryptEnabled,
            OnPreferEncryptChangedListener onPreferEncryptChangedListener) {
        super(context);

        this.onPreferEncryptChangedListener = onPreferEncryptChangedListener;
        this.preferEncryptEnabled = preferEncryptEnabled;

        LayoutInflater inflater = LayoutInflater.from(context);

        @SuppressLint("InflateParams")
        View contentView = inflater.inflate(R.layout.dialog_autocrypt_prefer_encrypt, null);

        TextView learnMoreText = (TextView) contentView.findViewById(R.id.prefer_encrypt_learn_more);
        makeTextViewLinksClickable(learnMoreText);

        preferEncryptCheckbox = (CheckBox) contentView.findViewById(R.id.prefer_encrypt_check);
        preferEncryptCheckbox.setChecked(preferEncryptEnabled);

        contentView.findViewById(R.id.prefer_encrypt).setOnClickListener(this);

        // TODO add autocrypt logo?
        // setIcon(R.drawable.autocrypt);
        setView(contentView);
        setButton(Dialog.BUTTON_NEUTRAL, context.getString(R.string.done_action), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                cancel();
            }
        });
    }

    @Override
    public void onClick(View v) {
        toggleCheck();
    }

    private void toggleCheck() {
        preferEncryptEnabled = !preferEncryptEnabled;
        preferEncryptCheckbox.setChecked(preferEncryptEnabled);

        onPreferEncryptChangedListener.onPreferEncryptChanged(preferEncryptEnabled);
    }

    public interface OnPreferEncryptChangedListener {
        void onPreferEncryptChanged(boolean enabled);
    }

    private void makeTextViewLinksClickable(TextView textView) {
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
