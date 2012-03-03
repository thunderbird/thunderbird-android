package com.fsck.k9.activity.setup;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.fsck.k9.*;
import com.fsck.k9.activity.K9ListActivity;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.SectionListAdapter;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashSet;

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
    private boolean bMakeDefault = true;

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
                dialog.setTitle(R.string.account_setup_dialog_new_title);

                final EditText emailField = ((EditText)dialog.findViewById(R.id.account_dialog_address_field));
                final EditText passwordField = ((EditText)dialog.findViewById(R.id.account_dialog_password_field));
                final CheckBox manualCheck = (CheckBox)dialog.findViewById(R.id.account_dialog_manual_box);
                final CheckBox defaultCheck = (CheckBox)dialog.findViewById(R.id.account_dialog_default_box);

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
                ((TextView)dialog.findViewById(R.id.account_dialog_password_help))
                        .setText(R.string.account_setup_dialog_enter_password);

                final EditText passwordField = ((EditText) dialog.findViewById(R.id.account_dialog_password_field));
                final CheckBox manualCheck = (CheckBox)dialog.findViewById(R.id.account_dialog_manual_box);
                final CheckBox defaultCheck = (CheckBox)dialog.findViewById(R.id.account_dialog_default_box);

                mPasswordDialogButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View view) {
                        String email = args.get(BUNDLE_TYPE_SUGGESTION).toString();
                        String password = passwordField.getText().toString();
                        bMakeDefault = defaultCheck.isChecked();
                        		
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
        AccountSetupAccountType.actionStartManualConfiguration(this, email, password, bMakeDefault);
        finish();
    }

    private void startSettingsDetection(String email, String password) {
        AccountSetupAutoConfiguration.actionAttemptConfiguration(this, email, password, bMakeDefault);
        finish();
    }

    /*
        Scans device for email addresses and adds them as suggestions
     */
    private void fillFromDevice(SectionListAdapter<AccountSuggestion> adp){

    	HashSet<String> tmpDeDup = new HashSet<String>();
    	
        for( android.accounts.Account acc : AccountManager.get(this).getAccounts() )
            if( mEmailValidator.isValidAddressOnly(acc.name) )
            	if( tmpDeDup.add(acc.name) )
            		adp.add(getString(R.string.account_setup_device_sectionheader), new AccountSuggestion(acc.name, SuggestionType.DEVICE));

        // TEMP DATA TO TEST
        /*accountAdapter.add("file1", new AccountSuggestion("piet@snot.com",SuggestionType.BACKUP));
        accountAdapter.add("file1", new AccountSuggestion("piet@snot.com",SuggestionType.BACKUP));
        accountAdapter.add("file1", new AccountSuggestion("piet@snot.com",SuggestionType.BACKUP));
        accountAdapter.add("file2", new AccountSuggestion("piet@snot.com",SuggestionType.BACKUP));
        accountAdapter.add("file2", new AccountSuggestion("piet@snot.com",SuggestionType.BACKUP));
        accountAdapter.add("file3", new AccountSuggestion("piet@snot.com",SuggestionType.BACKUP));*/
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

