package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import app.k9mail.legacy.account.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.ui.R;
import com.fsck.k9.ui.base.K9Activity;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.radiobutton.MaterialRadioButton;


public class AccountSetupComposition extends K9Activity {

    private static final String EXTRA_ACCOUNT = "account";

    private Account mAccount;

    private EditText mAccountSignature;
    private EditText mAccountEmail;
    private EditText mAccountAlwaysBcc;
    private EditText mAccountName;
    private MaterialCheckBox mAccountSignatureUse;
    private MaterialRadioButton mAccountSignatureBeforeLocation;
    private MaterialRadioButton mAccountSignatureAfterLocation;
    private LinearLayout mAccountSignatureLayout;


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
        mAccount = Preferences.getPreferences().getAccount(accountUuid);

        setLayout(R.layout.account_setup_composition);
        setTitle(R.string.account_settings_composition_title);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
            mAccount = Preferences.getPreferences().getAccount(accountUuid);
        }

        mAccountName = findViewById(R.id.account_name);
        mAccountName.setText(mAccount.getSenderName());

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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveSettings() {
        mAccount.setEmail(mAccountEmail.getText().toString());
        mAccount.setAlwaysBcc(mAccountAlwaysBcc.getText().toString());
        mAccount.setSenderName(mAccountName.getText().toString());
        mAccount.setSignatureUse(mAccountSignatureUse.isChecked());
        if (mAccountSignatureUse.isChecked()) {
            mAccount.setSignature(mAccountSignature.getText().toString());
            boolean isSignatureBeforeQuotedText = mAccountSignatureBeforeLocation.isChecked();
            mAccount.setSignatureBeforeQuotedText(isSignatureBeforeQuotedText);
        }

        Preferences.getPreferences().saveAccount(mAccount);
    }

    @Override
    public void onStop() {
        // TODO: Instead of saving the changes when the activity is stopped, add buttons to explicitly save or discard
        //  changes.
        saveSettings();
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_ACCOUNT, mAccount.getUuid());
    }
}
