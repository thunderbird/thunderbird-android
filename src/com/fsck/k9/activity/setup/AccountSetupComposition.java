package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.RadioButton;
import com.fsck.k9.Account;
import com.fsck.k9.K9Activity;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;

public class AccountSetupComposition extends K9Activity
{

    private static final String EXTRA_ACCOUNT = "account";

    private Account mAccount;

    private EditText mAccountSignature;
    private EditText mAccountEmail;
    private EditText mAccountAlwaysBcc;
    private EditText mAccountName;
    private RadioButton mAccountSignatureBeforeLocation;
    private RadioButton mAccountSignatureAfterLocation;


    public static void actionEditCompositionSettings(Activity context, Account account)
    {
        Intent i = new Intent(context, AccountSetupComposition.class);
        i.setAction(Intent.ACTION_EDIT);
        i.putExtra(EXTRA_ACCOUNT, account);
        context.startActivity(i);
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mAccount = (Account)getIntent().getSerializableExtra(EXTRA_ACCOUNT);

        setContentView(R.layout.account_setup_composition);

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT))
        {
            mAccount = (Account)savedInstanceState.getSerializable(EXTRA_ACCOUNT);
        }

        mAccountName = (EditText)findViewById(R.id.account_name);
        mAccountName.setText(mAccount.getName());

        mAccountEmail = (EditText)findViewById(R.id.account_email);
        mAccountEmail.setText(mAccount.getEmail());

        mAccountAlwaysBcc = (EditText)findViewById(R.id.account_always_bcc);
        mAccountAlwaysBcc.setText(mAccount.getAlwaysBcc());

        mAccountSignature = (EditText)findViewById(R.id.account_signature);
        mAccountSignature.setText(mAccount.getSignature());

        mAccountSignatureBeforeLocation = (RadioButton)findViewById(R.id.account_signature_location_before_quoted_text);
        mAccountSignatureAfterLocation = (RadioButton)findViewById(R.id.account_signature_location_after_quoted_text);
        boolean isSignatureBeforeQuotedText = mAccount.isSignatureBeforeQuotedText();
        mAccountSignatureBeforeLocation.setChecked(isSignatureBeforeQuotedText);
        mAccountSignatureAfterLocation.setChecked(!isSignatureBeforeQuotedText);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        mAccount.refresh(Preferences.getPreferences(this));
    }

    private void saveSettings()
    {
        mAccount.setEmail(mAccountEmail.getText().toString());
        mAccount.setAlwaysBcc(mAccountAlwaysBcc.getText().toString());
        mAccount.setName(mAccountName.getText().toString());
        mAccount.setSignature(mAccountSignature.getText().toString());
        boolean isSignatureBeforeQuotedText = mAccountSignatureBeforeLocation.isChecked();
        mAccount.setSignatureBeforeQuotedText(isSignatureBeforeQuotedText);

        mAccount.save(Preferences.getPreferences(this));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            saveSettings();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_ACCOUNT, mAccount);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        mAccount.save(Preferences.getPreferences(this));
        finish();
    }
}
