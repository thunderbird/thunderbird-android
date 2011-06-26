package com.fsck.k9.activity.setup;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.fsck.k9.EmailAddressValidator;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9ListActivity;
import com.fsck.k9.helper.SectionListAdapter;

import static com.fsck.k9.activity.setup.AccountSetupBasics.*;

/**
 * User: dzan
 * Date: 18/06/11
 */

public class AccountSetupIndex extends K9ListActivity implements OnItemClickListener, OnClickListener {

    private static final String DEVICE_STRING = "Device";
    private static final int DIALOG_NEW_ACCOUNT = 0;
    private static final int DIALOG_DEVICE_ACCOUNT = 1;
    private static final int DIALOG_BACKUP_ACCOUNT = 2;

    public enum SuggestionType { DEVICE, BACKUP, NEW }

    public static void actionNewAccount(Context context) {
        Intent i = new Intent(context, AccountSetupIndex.class);
        context.startActivity(i);
    }

    private Button mNewAccountButton;
    private SectionListAdapter<AccountSuggestion> accountAdapter;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // layout
        setContentView(R.layout.account_setup_index);
        mNewAccountButton = (Button)findViewById(R.id.new_account);

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

    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
          Toast.makeText(getApplicationContext(), ((TextView) view).getText(),
                  Toast.LENGTH_SHORT).show();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.new_account:
                onNew();
                break;
        }
    }

    protected void onNew() {
        AccountSetupBasics.actionNewAccount(this);
    }

    /*
        Dialogues
     */
    protected Dialog onCreateDialog(int dialog_id){
        Dialog dialog;

        switch (dialog_id){
            case DIALOG_NEW_ACCOUNT:
                dialog = new Dialog(this);
                break;
            case DIALOG_DEVICE_ACCOUNT:
                dialog = new Dialog(this);
                break;
            case DIALOG_BACKUP_ACCOUNT:
                dialog = new Dialog(this);
                break;
            default:
                dialog = null;
        }

        return dialog;
    }

    /*
        Scans device for email addresses and adds them as suggestions
     */
    private void fillFromDevice(SectionListAdapter<AccountSuggestion> adp){
        EmailAddressValidator mEmailValidator = new EmailAddressValidator();
        for( android.accounts.Account acc : AccountManager.get(this).getAccounts() )
            if( mEmailValidator.isValidAddressOnly(acc.name) )
                adp.add(DEVICE_STRING, new AccountSuggestion(acc.name, SuggestionType.DEVICE));

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
