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
import com.fsck.k9.ui.R;
import com.fsck.k9.activity.K9Activity;

public class AccountSetupComposition extends K9Activity {

    private static final String EXTRA_ACCOUNT = "account";

    private Account mAccount;

    private EditText mAccountSignature;
    private EditText mAccountEmail;
    private EditText mAccountAlwaysBcc;
    private EditText mAccountName;
    private CheckBox mAccountSignatureUse;
    private RadioButton mAccountSignatureBeforeLocation;
    private RadioButton mAccountSignatureAfterLocation;
    private LinearLayout mAccountSignatureLayout;

    public static void actionEditCompositionSettings(Activity context, Account account) {
        Intent i = new Intent(context, AccountSetupComposition.class);
        i.setAction(Intent.ACTION_EDIT);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        context.startActivity(i);
    }

    public static void actionEditCompositionSettings(Activity context, String accountUuid) {
        Intent intent = new Intent(context, AccountSetupComposition.class);
        intent.setAction(Intent.ACTION_EDIT);
        intent.putExtra(EXTRA_ACCOUNT, accountUuid);
        context.startActivity(intent);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        setContentView(R.layout.account_setup_composition);

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
            mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        }

        mAccountName = findViewById(R.id.account_name);
        mAccountName.setText(mAccount.getName());

        mAccountEmail = findViewById(R.id.account_email);
        mAccountEmail.setText(mAccount.getEmail());

        mAccountAlwaysBcc = findViewById(R.id.account_always_bcc);
        mAccountAlwaysBcc.setText(mAccount.getAlwaysBcc());

        mAccountSignatureLayout = findViewById(R.id.account_signature_layout);

        mAccountSignatureUse = findViewById(R.id.account_signature_use);
        boolean useSignature = mAccount.getSignatureUse();
        mAccountSignatureUse.setChecked(useSignature);
        mAccountSignatureUse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mAccountSignatureLayout.setVisibility(View.VISIBLE);
                    mAccountSignature.setText(mAccount.getSignature());
                    boolean isSignatureBeforeQuotedText = mAccount.isSignatureBeforeQuotedText();
                    mAccountSignatureBeforeLocation.setChecked(isSignatureBeforeQuotedText);
                    mAccountSignatureAfterLocation.setChecked(!isSignatureBeforeQuotedText);
                } else {
                    mAccountSignatureLayout.setVisibility(View.GONE);
                }
            }
        });

        mAccountSignature = findViewById(R.id.account_signature);

        mAccountSignatureBeforeLocation = findViewById(R.id.account_signature_location_before_quoted_text);
        mAccountSignatureAfterLocation = findViewById(R.id.account_signature_location_after_quoted_text);

        if (useSignature) {
            mAccountSignature.setText(mAccount.getSignature());

            boolean isSignatureBeforeQuotedText = mAccount.isSignatureBeforeQuotedText();
            mAccountSignatureBeforeLocation.setChecked(isSignatureBeforeQuotedText);
            mAccountSignatureAfterLocation.setChecked(!isSignatureBeforeQuotedText);
        } else {
            mAccountSignatureLayout.setVisibility(View.GONE);
        }
    }

    private void saveSettings() {
        mAccount.setEmail(mAccountEmail.getText().toString());
        mAccount.setAlwaysBcc(mAccountAlwaysBcc.getText().toString());
        mAccount.setName(mAccountName.getText().toString());
        mAccount.setSignatureUse(mAccountSignatureUse.isChecked());
        if (mAccountSignatureUse.isChecked()) {
            mAccount.setSignature(mAccountSignature.getText().toString());
            boolean isSignatureBeforeQuotedText = mAccountSignatureBeforeLocation.isChecked();
            mAccount.setSignatureBeforeQuotedText(isSignatureBeforeQuotedText);
        }

        mAccount.save();
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
        mAccount.save();
        finish();
    }
}
