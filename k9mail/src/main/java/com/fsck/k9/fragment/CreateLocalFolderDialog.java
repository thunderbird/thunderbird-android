package com.fsck.k9.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.helper.NotInSetValidator;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalStore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ConteDiMonteCristo on 15/07/15.
 * This class manages the input for a local folder name
 * to be create, deleted or edited
 */
public class CreateLocalFolderDialog extends DialogFragment
{
    private static final String ARG_TITLE = "title";
    private EditText editFolderName;
    private TextView textMessage;
    private Button bOk;
    private FolderNameValidation mValidator;

    private void setValidator(FolderNameValidation mValidator) {
        this.mValidator = mValidator;
    }

    /**
     * Interface for validating the text in input
     */
    public interface FolderNameValidation{
        Boolean validateName(String field);
    }


    //todo: add list of folder names

    /**
     * Create a new instance of this dialog to input the name of the new folder
     * @param title the title of the dialog
     * @param store a reference to the local store
     * @return a CreateLocalFolderDialog
     */
    public static CreateLocalFolderDialog newInstance(String title, LocalStore store) throws MessagingException {

        CreateLocalFolderDialog fragment = new CreateLocalFolderDialog();

        List<? extends Folder> folders = store.getPersonalNamespaces(true);
        Set<String> fnames = new HashSet<String>();
        for (Folder f:folders) fnames.add(f.getName());

        FolderNameValidation validator = new NotInSetValidator(fnames);


        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);

        fragment.setArguments(args);
        fragment.setValidator(validator);
        fragment.setStyle(STYLE_NORMAL,0);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.local_folder_dialog_fragment, null);
        textMessage = (TextView)v.findViewById(R.id.local_folder_validation);
        editFolderName = (EditText)v.findViewById(R.id.local_folder_name);
        editFolderName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mValidator == null) return;
                if (mValidator.validateName(s.toString())) {
                    bOk.setEnabled(true);
                    textMessage.setText("");
                } else {
                    bOk.setEnabled(false);
                    if (!s.toString().isEmpty())
                        textMessage.setText(R.string.existing_local_folder_name);
                }
            }
        });
        builder.setTitle(getArguments().getString(ARG_TITLE))
        .setView(v)
        .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {clickOk(dialog, id);}
        })
        .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                clickCancel(dialog, id);
            }

        });

        return builder.create();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        AlertDialog ad = (AlertDialog)getDialog();
        if (ad == null) return;
        bOk = ad.getButton(AlertDialog.BUTTON_POSITIVE);
        bOk.setEnabled(false);
    }

    private void clickOk(DialogInterface dialog, int id)
    {
        String folderName = editFolderName.getText().toString();
        Log.i(K9.LOG_TAG, String.format("Local folder to be created: %s",folderName));
    }

    private void clickCancel(DialogInterface dialog, int id)
    {

    }


}
