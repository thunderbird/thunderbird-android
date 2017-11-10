package com.fsck.k9.fragment;

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

public class KeywordEditNameDialogFragment extends DialogFragment {

    public interface KeywordEditNameDialogListener {
        public void onKeywordNameChanged(String name, int id);
    }

    private Keyword keyword;
    private int position;
    private String name;
    private EditText nameEdit;
    private KeywordEditNameDialogListener listener;
    private Button positiveButton;

    public void setKeyword(Keyword keyword, int position) {
        this.keyword = keyword;
        this.position = position;
        name = keyword.getName();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("name", nameEdit.getText().toString());
        outState.putSerializable("position", position);
        outState.putParcelable("keyword", keyword);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstance) {

        if (savedInstance != null) {
            name = (String) savedInstance.getSerializable("name");
            position = (Integer) savedInstance.getSerializable("position");
            keyword = (Keyword) savedInstance.getParcelable("keyword");
        }

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.keyword_edit_name, null);

        nameEdit = (EditText) view.findViewById(R.id.keyword_name_edit);
        nameEdit.addTextChangedListener(new NameWatcher());
        nameEdit.setText(name);
        nameEdit.setSelection(name.length());
        final EditText nameEditFinal = nameEdit;

        TextView externalCode =
            (TextView) view.findViewById(R.id.keyword_external_code);
        externalCode.setText(keyword.getExternalCode());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.keyword_edit_name_dialog_title)
            .setView(view)
            .setPositiveButton(android.R.string.ok, new OkListener())
            .setNegativeButton(android.R.string.cancel, null);

        Dialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (KeywordEditNameDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                "does not implement KeywordEditNameDialogListener");
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
        if (positiveButton == null) {
            return false;
        }
        final String name = nameEdit.getText().toString();
        return (name.length() != 0);
    }

    private class NameWatcher implements TextWatcher {
        @Override
        public void afterTextChanged(Editable s) {
            if (positiveButton != null) {
                positiveButton.setEnabled(isInputValid());
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
    }

    private class OkListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int id) {
            listener.onKeywordNameChanged(
                nameEdit.getText().toString(), position);
        }
    }
}
