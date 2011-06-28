package com.fsck.k9.activity.setup;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.fsck.k9.*;
import com.fsck.k9.activity.K9ListActivity;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.SectionListAdapter;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import static com.fsck.k9.activity.setup.AccountSetupBasics.*;

/**
 * User: dzan
 * Date: 18/06/11
 *
 * TODO: Encapsulate accountAdaptor. No need for this to be global except due the test-data.
 */

public class AccountSetupIndex extends K9ListActivity implements OnItemClickListener, OnClickListener {

    private static final int DIALOG_NEW_ACCOUNT = 0;
    private static final int DIALOG_DEVICE_ACCOUNT = 1;
    private static final int DIALOG_BACKUP_ACCOUNT = 2;

    private static final String BUNDLE_TYPE_SUGGESTION = "SUGGESTION";
    public enum SuggestionType { DEVICE, BACKUP, NEW }

    // temp hard coded value ( since we don't ask if it should be default account for now )
    private boolean bTmpDefaultAccount = true;

    private Account mAccount;
    private Button mNewAccountDialogButton;
    private Button mPasswordDialogButton;
    private EmailAddressValidator mEmailValidator = new EmailAddressValidator();

    public static void actionNewAccount(Context context) {
        Intent i = new Intent(context, AccountSetupIndex.class);
        context.startActivity(i);
    }

    private SectionListAdapter<AccountSuggestion> accountAdapter;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // layout
        setContentView(R.layout.account_setup_index);
        Button mNewAccountButton = (Button)findViewById(R.id.new_account);

        // button callbacks
        mNewAccountButton.setOnClickListener(this);

        // creating the adaptor and start filling it
        accountAdapter = new SectionListAdapter<AccountSuggestion>(this,
                R.layout.account_setup_index_list_header,
                R.layout.account_setup_index_list_item);
        fillFromDevice(accountAdapter);
        new BackupScan().execute();

        // configuring the view
        ListView listView = getListView();
        listView.setOnItemClickListener(this);
        listView.setAdapter(accountAdapter);

    }

    public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
        SectionListAdapter<AccountSuggestion> adapter = (SectionListAdapter<AccountSuggestion>)adapterView.getAdapter();
        AccountSuggestion suggestion = (AccountSuggestion)adapter.getItem(pos);

        Bundle tmp = new Bundle();
        tmp.putString(BUNDLE_TYPE_SUGGESTION,suggestion.getAccount());

        switch(suggestion.getSuggestionType()){
            case DEVICE:
                showDialog(DIALOG_DEVICE_ACCOUNT,tmp);
                break;
            case BACKUP:
                showDialog(DIALOG_BACKUP_ACCOUNT,tmp);
                break;
            default:
                return;
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.new_account:
                showDialog(DIALOG_NEW_ACCOUNT);
                break;
        }
    }

    /*
        Dialogues
     */
    protected Dialog onCreateDialog(int dialog_id){

        Dialog dialog = new Dialog(this);

        switch(dialog_id){
            case DIALOG_NEW_ACCOUNT:
                dialog.setContentView(R.layout.account_dialog_new);
                dialog.setTitle("Setup a new account.");

                final EditText emailField = ((EditText)dialog.findViewById(R.id.account_dialog_address_field));
                final EditText passwordField = ((EditText)dialog.findViewById(R.id.account_dialog_password_field));
                final CheckBox manualCheck = (CheckBox)dialog.findViewById(R.id.account_dialog_manual_box);

                mNewAccountDialogButton = ((Button)dialog.findViewById(R.id.account_dialog_next));

                mNewAccountDialogButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View view) {
                        String email = emailField.getText().toString();
                        String password = passwordField.getText().toString();

                        // TODO: replace this with a few listeners on the fields that activate/deactive the button on acceptable values
                        if( !mEmailValidator.isValidAddressOnly(email) || password.isEmpty() ) return;

                        if(manualCheck.isChecked())
                            onManualSetup(email, password);
                        else startSettingsDetection(email, password);
                    }
                });
                break;

            case DIALOG_DEVICE_ACCOUNT:
            case DIALOG_BACKUP_ACCOUNT:
                dialog.setContentView(R.layout.account_dialog_password);
                dialog.setTitle("Enter password.");
                mPasswordDialogButton = ((Button)dialog.findViewById(R.id.account_dialog_next));
                break;

            default: dialog = null;
        }

        dialog.show();
        return dialog;
    }

    protected void onPrepareDialog(int id, Dialog dialog, final Bundle args){
        switch(id){
            case DIALOG_DEVICE_ACCOUNT:
            case DIALOG_BACKUP_ACCOUNT:
                ((TextView)dialog.findViewById(R.id.account_dialog_password_help)).setText
                        ("Enter the password for the '" + args.get(BUNDLE_TYPE_SUGGESTION) + "' account: ");

                final EditText passwordField = ((EditText) dialog.findViewById(R.id.account_dialog_password_field));
                final CheckBox manualCheck = (CheckBox)dialog.findViewById(R.id.account_dialog_manual_box);

                mPasswordDialogButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View view) {
                        String email = args.get(BUNDLE_TYPE_SUGGESTION).toString();
                        String password = passwordField.getText().toString();

                        // TODO: replace this with a few listeners on the fields that activate/deactive the button on acceptable values
                        if( password.isEmpty() ) return;

                        if (manualCheck.isChecked())
                            onManualSetup(email, password);
                        else startSettingsDetection(email, password);
                    }
                });
                break;
            default: return;
        }
    }

    private void onManualSetup(String email, String password) {
        String[] emailParts = splitEmail(email);
        String user = emailParts[0];
        String domain = emailParts[1];

        mAccount = Preferences.getPreferences(this).newAccount();
        mAccount.setName(getOwnerName());
        mAccount.setEmail(email);
        try {
            String userEnc = URLEncoder.encode(user, "UTF-8");
            String passwordEnc = URLEncoder.encode(password, "UTF-8");

            URI uri = new URI("placeholder", userEnc + ":" + passwordEnc, "mail." + domain, -1, null,
                              null, null);
            mAccount.setStoreUri(uri.toString());
            mAccount.setTransportUri(uri.toString());
        } catch (UnsupportedEncodingException enc) {
            // This really shouldn't happen since the encoding is hardcoded to UTF-8
            Log.e(K9.LOG_TAG, "Couldn't urlencode username or password.", enc);
        } catch (URISyntaxException use) {
            /*
             * If we can't set up the URL we just continue. It's only for
             * convenience.
             */
        }
        mAccount.setDraftsFolderName(getString(R.string.special_mailbox_name_drafts));
        mAccount.setTrashFolderName(getString(R.string.special_mailbox_name_trash));
        mAccount.setSentFolderName(getString(R.string.special_mailbox_name_sent));

        AccountSetupAccountType.actionSelectAccountType(this, mAccount, bTmpDefaultAccount);
        finish();
    }

    private void startSettingsDetection(String email, String password) {

    }

    /*
        Scans device for email addresses and adds them as suggestions
     */
    private void fillFromDevice(SectionListAdapter<AccountSuggestion> adp){

        for( android.accounts.Account acc : AccountManager.get(this).getAccounts() )
            if( mEmailValidator.isValidAddressOnly(acc.name) )
                adp.add(getString(R.string.account_setup_device_sectionheader), new AccountSuggestion(acc.name, SuggestionType.DEVICE));

        // TEMP DATA TO TEST
        accountAdapter.add("file1", new AccountSuggestion("piet@snot.com",SuggestionType.BACKUP));
        accountAdapter.add("file1", new AccountSuggestion("piet@snot.com",SuggestionType.BACKUP));
        accountAdapter.add("file1", new AccountSuggestion("piet@snot.com",SuggestionType.BACKUP));
        accountAdapter.add("file2", new AccountSuggestion("piet@snot.com",SuggestionType.BACKUP));
        accountAdapter.add("file2", new AccountSuggestion("piet@snot.com",SuggestionType.BACKUP));
        accountAdapter.add("file3", new AccountSuggestion("piet@snot.com",SuggestionType.BACKUP));
    }

    /*
        Task to scan for backup accounts and add them to the list
    */
    private class BackupScan extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... objects) {
           // scan for .k9s files
           // scan each file for accounts
           // add them to the list   ( with addBackupAccount )
           return true;
        }
    }

    /*
        Some helpers copy pasted from previous setup. Need a review.
     */
    private String[] splitEmail(String email) {
        String[] retParts = new String[2];
        String[] emailParts = email.split("@");
        retParts[0] = (emailParts.length > 0) ? emailParts[0] : "";
        retParts[1] = (emailParts.length > 1) ? emailParts[1] : "";
        return retParts;
    }

    private String getOwnerName() {
        String name = null;
        try {
            name = Contacts.getInstance(this).getOwnerName();
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Could not get owner name, using default account name", e);
        }
        if (name == null || name.length() == 0) {
            try {
                name = getDefaultAccountName();
            } catch (Exception e) {
                Log.e(K9.LOG_TAG, "Could not get default account name", e);
            }
        }
        if (name == null) {
            name = "";
        }
        return name;
    }

    private String getDefaultAccountName() {
        String name = null;
        Account account = Preferences.getPreferences(this).getDefaultAccount();
        if (account != null) {
            name = account.getName();
        }
        return name;
    }

    /*
        Simple class to wrap account suggestion data
     */
    private class AccountSuggestion
    {
        private String account;
        private SuggestionType type;

        public AccountSuggestion(String account, SuggestionType type){
            this.account = account;
            this.type = type;
        }

        public String toString(){ return account; }
        public String getAccount(){ return account; }
        public SuggestionType getSuggestionType(){ return type; }
    }
}
