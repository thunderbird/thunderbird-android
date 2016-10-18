package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;

public class AccountSetupReportSpam extends K9Activity {

    private static final String EXTRA_ACCOUNT = "account";

    private Account mAccount;

    private EditText mAccountReportSpamRecipient;
    private EditText mAccountReportSpamSubject;
    private CheckBox mAccountReportSpamDelete;

    public static void actionEditReportSpamSettings(Activity context, Account account) {
        Intent i = new Intent(context, AccountSetupReportSpam.class);
        i.setAction(Intent.ACTION_EDIT);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        setContentView(R.layout.account_setup_reportspam);

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
            mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        }

        mAccountReportSpamRecipient = (EditText)findViewById(R.id.account_reportspam_recipient);
        mAccountReportSpamRecipient.setText(mAccount.getReportSpamRecipient());

        mAccountReportSpamSubject = (EditText)findViewById(R.id.account_reportspam_subject);
        mAccountReportSpamSubject.setText(mAccount.getReportSpamSubject());

        mAccountReportSpamDelete = (CheckBox)findViewById(R.id.account_reportspam_delete);
        boolean useSignature = mAccount.isReportSpamDelete();
        mAccountReportSpamDelete.setChecked(useSignature);
    }

    private void saveSettings() {
        mAccount.setReportSpamRecipient(mAccountReportSpamRecipient.getText().toString());
        mAccount.setReportSpamSubject(mAccountReportSpamSubject.getText().toString());
        mAccount.setReportSpamDelete(mAccountReportSpamDelete.isChecked());

        mAccount.save(Preferences.getPreferences(this));
    }

    @Override
    public void onBackPressed() {
        saveSettings();
        super.onBackPressed();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_ACCOUNT, mAccount.getUuid());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mAccount.save(Preferences.getPreferences(this));
        finish();
    }
}
