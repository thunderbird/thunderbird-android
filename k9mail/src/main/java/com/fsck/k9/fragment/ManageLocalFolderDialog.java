package com.fsck.k9.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.fsck.k9.R;

/**
 * Created by ConteDiMonteCristo on 15/07/15.
 * This class manages the input for a local folder name
 * to be create, deleted or edited
 */
public class ManageLocalFolderDialog extends DialogFragment
{
    protected static final String ARG_TITLE = "title";
    protected static final String FOLDER_NAME = "folderName";
    private String mFolderName;
    private EditText editFolderName;

    public interface FolderNameValidation{
        public Boolean validateName(View textField);
    }


    //todo: add list of folder names
    public static ManageLocalFolderDialog newInstance(String title, FolderNameValidation validator)
    {
        ManageLocalFolderDialog fragment = new ManageLocalFolderDialog();

        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(FOLDER_NAME, "");

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.local_folder_dialog_fragment, null);
        editFolderName = (EditText)v.findViewById(R.id.local_folder_name);
        builder.setView(v)
        // Add action buttons
        .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {clickOk(dialog, id);}
        })
        .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {clickCancel(dialog, id);}

        });
        return builder.create();
    }

    private void clickOk(DialogInterface dialog, int id)
    {
        mFolderName = editFolderName.getText().toString();
    }

    private void clickCancel(DialogInterface dialog, int id)
    {

    }


}
