package com.android.email.activity;

import java.util.List;

import com.android.email.Account;
import com.android.email.Preferences;
import com.android.email.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.EditText;

public class EditIdentity extends Activity
{

    public static final String EXTRA_IDENTITY = "com.android.email.EditIdentity_identity";
    public static final String EXTRA_IDENTITY_INDEX = "com.android.email.EditIdentity_identity_index";
    public static final String EXTRA_ACCOUNT = "com.android.email.EditIdentity_account";

    private Account mAccount;
    private Account.Identity mIdentity;
    private int mIdentityIndex;
    private EditText mDescriptionView;
    private EditText mSignatureView;
    private EditText mEmailView;
//  private EditText mAlwaysBccView;
    private EditText mNameView;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mIdentity = (Account.Identity)getIntent().getSerializableExtra(EXTRA_IDENTITY);
        mIdentityIndex = getIntent().getIntExtra(EXTRA_IDENTITY_INDEX, -1);
        mAccount = (Account) getIntent().getSerializableExtra(EXTRA_ACCOUNT);

        if (mIdentityIndex == -1)
        {
            mIdentity = mAccount.new Identity();
        }

        setContentView(R.layout.edit_identity);

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_IDENTITY))
        {
            mIdentity = (Account.Identity)savedInstanceState.getSerializable(EXTRA_IDENTITY);
        }

        mDescriptionView = (EditText)findViewById(R.id.description);
        mDescriptionView.setText(mIdentity.getDescription());

        mNameView = (EditText)findViewById(R.id.name);
        mNameView.setText(mIdentity.getName());

        mEmailView = (EditText)findViewById(R.id.email);
        mEmailView.setText(mIdentity.getEmail());

//      mAccountAlwaysBcc = (EditText)findViewById(R.id.bcc);
//      mAccountAlwaysBcc.setText(mIdentity.getAlwaysBcc());

        mSignatureView = (EditText)findViewById(R.id.signature);
        mSignatureView.setText(mIdentity.getSignature());
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    private void saveIdentity()
    {

        mIdentity.setDescription(mDescriptionView.getText().toString());
        mIdentity.setEmail(mEmailView.getText().toString());
        //      mIdentity.setAlwaysBcc(mAccountAlwaysBcc.getText().toString());
        mIdentity.setName(mNameView.getText().toString());
        mIdentity.setSignature(mSignatureView.getText().toString());

        List<Account.Identity> identities = mAccount.getIdentities();
        if (mIdentityIndex == -1)
        {
            identities.add(mIdentity);
        }
        else
        {
            identities.remove(mIdentityIndex);
            identities.add(mIdentityIndex, mIdentity);
        }

        mAccount.save(Preferences.getPreferences(getApplication().getApplicationContext()));

        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            saveIdentity();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_IDENTITY, mIdentity);
    }
}
