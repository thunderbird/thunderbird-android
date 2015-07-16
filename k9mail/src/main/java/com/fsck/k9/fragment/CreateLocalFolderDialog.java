package com.fsck.k9.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.fsck.k9.R;

import java.util.Set;

/**
 * Created by ConteDiMonteCristo on 15/07/15.
 * This class manages the input for a local folder name
 * to be create, deleted or edited
 */
public class CreateLocalFolderDialog extends DialogFragment
{
    protected static final String ARG_TITLE = "title";
    protected static final String FOLDER_NAME = "folderName";
    protected static final String FOLDER_NAMES = "folderNames";
    private String mFolderName; //the selection for the folder name
    private String mTitle;
    private EditText editFolderName;
    private TextView textMessage;
    private Button bOk;
    private FolderNameValidation mValidator;


    public String getmFolderName() {
        return mFolderName;
    }

    public String getTitle() {return mTitle;}

    private void setmValidator(FolderNameValidation mValidator) {
        this.mValidator = mValidator;
    }

    /**
     * Interface for validating the text in input
     */
    public interface FolderNameValidation{
        public Boolean validateName(String field);
    }


    //todo: add list of folder names

    /**
     * Create a new instance of this dialog to input the name of the new folder
     * @param title the title of the dialog
     * @param validator a validator used to check that the name input is correct
     * @return a CreateLocalFolderDialog
     */
    public static CreateLocalFolderDialog newInstance(String title, FolderNameValidation validator)
    {
        CreateLocalFolderDialog fragment = new CreateLocalFolderDialog();

        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(FOLDER_NAME, "");

        fragment.setArguments(args);
        fragment.setmValidator(validator);

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
                    textMessage.setText(R.string.existing_local_folder_name);
                }
            }
        });
        builder.setTitle(mTitle)
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
        bOk = (Button)ad.getButton(AlertDialog.BUTTON_POSITIVE);
        bOk.setEnabled(false);
    }

    private void clickOk(DialogInterface dialog, int id)
    {
        mFolderName = editFolderName.getText().toString();
    }

    private void clickCancel(DialogInterface dialog, int id)
    {

    }


}
