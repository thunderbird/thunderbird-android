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

public class AccountSetupComposition extends K9Activity {

    private static final String EXTRA_ACCOUNT = "account";

    private Account account;

    private EditText accountSignature;
    private EditText accountEmail;
    private EditText accountAlwaysBcc;
    private EditText accountName;
    private CheckBox accountSignatureUse;
    private RadioButton accountSignatureBeforeLocation;
    private RadioButton accountSignatureAfterLocation;
    private LinearLayout accountSignatureLayout;

    public static void actionEditCompositionSettings(Activity context, Account account) {
        Intent i = new Intent(context, AccountSetupComposition.class);
        i.setAction(Intent.ACTION_EDIT);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        account = Preferences.getPreferences(this).getAccount(accountUuid);

        setContentView(R.layout.account_setup_composition);

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
            account = Preferences.getPreferences(this).getAccount(accountUuid);
        }

        accountName = (EditText)findViewById(R.id.account_name);
        accountName.setText(account.getName());

        accountEmail = (EditText)findViewById(R.id.account_email);
        accountEmail.setText(account.getEmail());

        accountAlwaysBcc = (EditText)findViewById(R.id.account_always_bcc);
        accountAlwaysBcc.setText(account.getAlwaysBcc());

        accountSignatureLayout = (LinearLayout)findViewById(R.id.account_signature_layout);

        accountSignatureUse = (CheckBox)findViewById(R.id.account_signature_use);
        boolean useSignature = account.getSignatureUse();
        accountSignatureUse.setChecked(useSignature);
        accountSignatureUse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    accountSignatureLayout.setVisibility(View.VISIBLE);
                    accountSignature.setText(account.getSignature());
                    boolean isSignatureBeforeQuotedText = account.isSignatureBeforeQuotedText();
                    accountSignatureBeforeLocation.setChecked(isSignatureBeforeQuotedText);
                    accountSignatureAfterLocation.setChecked(!isSignatureBeforeQuotedText);
                } else {
                    accountSignatureLayout.setVisibility(View.GONE);
                }
            }
        });

        accountSignature = (EditText)findViewById(R.id.account_signature);

        accountSignatureBeforeLocation = (RadioButton)findViewById(R.id.account_signature_location_before_quoted_text);
        accountSignatureAfterLocation = (RadioButton)findViewById(R.id.account_signature_location_after_quoted_text);

        if (useSignature) {
            accountSignature.setText(account.getSignature());

            boolean isSignatureBeforeQuotedText = account.isSignatureBeforeQuotedText();
            accountSignatureBeforeLocation.setChecked(isSignatureBeforeQuotedText);
            accountSignatureAfterLocation.setChecked(!isSignatureBeforeQuotedText);
        } else {
            accountSignatureLayout.setVisibility(View.GONE);
        }
    }

    private void saveSettings() {
        account.setEmail(accountEmail.getText().toString());
        account.setAlwaysBcc(accountAlwaysBcc.getText().toString());
        account.setName(accountName.getText().toString());
        account.setSignatureUse(accountSignatureUse.isChecked());
        if (accountSignatureUse.isChecked()) {
            account.setSignature(accountSignature.getText().toString());
            boolean isSignatureBeforeQuotedText = accountSignatureBeforeLocation.isChecked();
            account.setSignatureBeforeQuotedText(isSignatureBeforeQuotedText);
        }

        account.save(Preferences.getPreferences(this));
    }

    @Override
    public void onBackPressed() {
        saveSettings();
        super.onBackPressed();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_ACCOUNT, account.getUuid());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        account.save(Preferences.getPreferences(this));
        finish();
    }
}
