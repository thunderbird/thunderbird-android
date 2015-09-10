package com.fsck.k9.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.FolderList;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.helper.NotInSetValidator;
import com.fsck.k9.helper.RangeValidator;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
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
    private static final String ARG_TITLE = "TITLE";
    private static final String ARG_ACCOUNT_ID = "UUID";
    private EditText editFolderName;
    private TextView textMessage;
    private Button bOk;
    private RangeValidator mValidator;
    private LocalStore mStore;
    private Account mAccount;
    private Activity mParentActivity;
    private String mTitle;

    /**
     * Create a new instance of this dialog to input the name of the new folder
     * @param title the title of the dialog
     * @param account the present account
     * @return a CreateLocalFolderDialog
     */
    public static CreateLocalFolderDialog newInstance(String title, Account account) {

        CreateLocalFolderDialog fragment = new CreateLocalFolderDialog();
        String accountId = account.getUuid();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_ACCOUNT_ID, accountId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mParentActivity = getActivity();

        Bundle args = getArguments();
        if (args==null && savedInstanceState!=null)
            args = savedInstanceState;
        String aid = args!= null? args.getString(ARG_ACCOUNT_ID):null;
        if (aid==null) {
            Log.e(K9.LOG_TAG,"Could not create dialog to create a local folder");
            return null;
        }
        mAccount = Preferences.getPreferences(mParentActivity).getAccount(aid);

        try {
            mStore = mAccount.getLocalStore();
            List<? extends Folder> folders = mStore.getPersonalNamespaces(true);
            Set<String> fnames = new HashSet<String>();
            for (Folder f:folders) fnames.add(f.getName());
            mValidator = new NotInSetValidator(fnames);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View v = inflater.inflate(R.layout.local_folder_dialog_fragment,null);

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
                    if (bOk == null) return;
                    if (mValidator.validateField(s.toString())) {
                        bOk.setEnabled(true);
                        textMessage.setText("");
                    } else {
                        bOk.setEnabled(false);
                        if (!s.toString().isEmpty())
                            textMessage.setText(R.string.existing_local_folder_name);
                    }
                }
            });
            mTitle = args.getString(ARG_TITLE);
            builder.setTitle(mTitle)
                    .setView(v)
                    .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {clickOk();}
                    })
                    .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            clickCancel();
                        }

                    });

            return builder.create();

        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG,"Could not create dialog to create a local folder");
        }

        return null;

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_TITLE, mTitle);
        outState.putString(ARG_ACCOUNT_ID, mAccount.getUuid());
    }


    @Override
    public void onStart()
    {
        super.onStart();
        AlertDialog ad = (AlertDialog)getDialog();
        if (ad == null) return;
        bOk = ad.getButton(AlertDialog.BUTTON_POSITIVE);
        bOk.setEnabled(mValidator.validateField(editFolderName.getText().toString()));
    }


    private void clickOk()
    {
        String folderName = editFolderName.getText().toString();
        Log.i(K9.LOG_TAG, String.format("Local folder to be created: %s", folderName));
        LocalFolder lf = new LocalFolder(mStore,folderName);

        try {
            lf.create(Folder.FolderType.HOLDS_MESSAGES);
            lf.setSyncClass(Folder.FolderClass.LOCAL);
            lf.setDisplayClass(Folder.FolderClass.FIRST_CLASS);
            lf.setStatus(getString(R.string.local_folder_status));
        } catch (MessagingException e) {
            Log.e(K9.LOG_TAG, "Unable to create a local folder", e);
        }
        Toast toast = Toast.makeText(this.getActivity(), String.format("Local folder %s created",folderName), Toast.LENGTH_SHORT);
        toast.show();
        ((FolderList)mParentActivity).enableDeleteLocalFolderItem();

        MessagingController.getInstance(getActivity().getApplication()).listFolders(mAccount, false, null);
    }

    private void clickCancel()
    {
        dismiss();
    }


}
