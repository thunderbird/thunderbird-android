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

import com.fsck.k9.R;

import java.util.ArrayList;
import java.util.List;

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
    private ArrayList<String> mFNames; //list of folder names
    private EditText editFolderName;
    private Button bOk;
    private FolderNameValidation mValidator;


    public String getmFolderName() {
        return mFolderName;
    }

    private void setmFNames(ArrayList<String> mFNames) {
        this.mFNames = mFNames;
    }

    private void setmValidator(FolderNameValidation mValidator) {
        this.mValidator = mValidator;
    }

    /**
     * Interface for validating the text in input
     */
    public interface FolderNameValidation{
        public Boolean validateName(Editable field);
    }


    //todo: add list of folder names

    /**
     * Create a new instance of this dialog to input the name of the new folder
     * @param title the title of the dialog
     * @param fnames a collection of names for the existing folders
     * @param validator a validator used to check that the name input is correct
     * @return a CreateLocalFolderDialog
     */
    public static CreateLocalFolderDialog newInstance(String title, List<String> fnames, FolderNameValidation validator)
    {
        CreateLocalFolderDialog fragment = new CreateLocalFolderDialog();

        ArrayList foldersNames = new ArrayList(fnames);

        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(FOLDER_NAME, "");
        args.putStringArrayList(FOLDER_NAMES, foldersNames);

        fragment.setArguments(args);
        fragment.setmFNames(foldersNames);
        fragment.setmValidator(validator);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.local_folder_dialog_fragment, null);
        editFolderName = (EditText)v.findViewById(R.id.local_folder_name);
        editFolderName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (mValidator.validateName(s)) {
                    bOk.setEnabled(true);
                }
                bOk.setEnabled(false);

            }
        });
        builder.setView(v)
        // Add action buttons
        .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {clickOk(dialog, id);}
        })
        .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                clickCancel(dialog, id);
            }

        });

        AlertDialog ad = builder.create();
        bOk = ad.getButton(AlertDialog.BUTTON_POSITIVE);
        return ad;
    }

    private void clickOk(DialogInterface dialog, int id)
    {
        mFolderName = editFolderName.getText().toString();
    }

    private void clickCancel(DialogInterface dialog, int id)
    {

    }


}
