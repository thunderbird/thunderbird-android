package com.fsck.k9.activity;

import com.fsck.k9.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class PasswordEntryDialog {
    public interface PasswordEntryListener {
        void passwordChosen(String chosenPassword);
        void cancel();
    }
    PasswordEntryListener listener;
    private EditText passwordView;
    AlertDialog dialog;
    public PasswordEntryDialog(Context context, String headerText, PasswordEntryListener listener) {
        this.listener = listener;
        View view = LayoutInflater.from(context).inflate(R.layout.password_entry_dialog, null);
        Builder builder = new AlertDialog.Builder(context);
        passwordView = (EditText)view.findViewById(R.id.password_text_box);

        builder.setView(view);
        builder.setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (PasswordEntryDialog.this.listener != null) {
                    String chosenPassword = passwordView.getText().toString();
                    PasswordEntryDialog.this.listener.passwordChosen(chosenPassword);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (PasswordEntryDialog.this.listener != null) {
                    PasswordEntryDialog.this.listener.cancel();
                }
            }
        });
        dialog = builder.create();
        passwordView.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) { }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2,
            int arg3) {

                Button okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                String chosenPassword = passwordView.getText().toString();
                okButton.setEnabled(chosenPassword.length() > 0);

            }
        });

        dialog.setMessage(headerText);


    }
    public void show() {
        dialog.show();
        Button okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        okButton.setEnabled(false);
    }

}
