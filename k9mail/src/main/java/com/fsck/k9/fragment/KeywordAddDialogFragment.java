package com.fsck.k9.fragment;

import java.lang.CharSequence;
import java.lang.ClassCastException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.fsck.k9.mail.Keyword;
import com.fsck.k9.R;

public class KeywordAddDialogFragment extends DialogFragment {

    public interface KeywordAddDialogFragmentDialogListener {
        public void onKeywordAdded(String externalCode);
    }

    private EditText externalCodeEdit;
    private TextView message;
    private KeywordAddDialogFragmentDialogListener listener;
    private Button positiveButton;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(
            "external_code", externalCodeEdit.getText().toString());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstance) {

        String externalCode = "";
        if (savedInstance != null) {
            externalCode =
                (String) savedInstance.getSerializable("external_code");
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.keyword_add, null);

        externalCodeEdit = (EditText) view.findViewById(
            R.id.keyword_external_code_edit);
        externalCodeEdit.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void afterTextChanged(Editable s) {
                if (positiveButton != null) {
                    final boolean valid = isInputValid();
                    positiveButton.setEnabled(valid);
                    message.setVisibility(
                        valid ? View.INVISIBLE : View.VISIBLE);
                }
            }
            @Override
            public void beforeTextChanged(
                CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(
                CharSequence s, int start, int before, int count) {
            }
        });
        externalCodeEdit.setText(externalCode);
        externalCodeEdit.setSelection(externalCode.length());
        final EditText externalCodeEditFinal = externalCodeEdit;

        message = (TextView) view.findViewById(R.id.keyword_add_message);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.keyword_add_dialog_title)
            .setView(view)
            .setPositiveButton(
                android.R.string.ok, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    if (isInputValid()) {
                        listener.onKeywordAdded(
                            externalCodeEditFinal.getText().toString());
                    }
                }
            })
            .setNegativeButton(android.R.string.cancel, null);

        final Dialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (KeywordAddDialogFragmentDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                "does not implement KeywordAddDialogFragmentDialogListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        final AlertDialog dialog = (AlertDialog) getDialog();
        positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setEnabled(isInputValid());
        }
    }

    private boolean isInputValid() {
        if (externalCodeEdit == null) {
            return false;
        }
        final String externalCode = externalCodeEdit.getText().toString();
        return (externalCode.length() != 0) &&
               Keyword.isValidImapKeyword(externalCode);
    }
}
