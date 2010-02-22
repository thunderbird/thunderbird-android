package com.fsck.k9.activity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9Activity;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;

public class EditIdentity extends K9Activity
{
    public static final String EXTRA_IDENTITY = "com.fsck.k9.EditIdentity_identity";
    public static final String EXTRA_IDENTITY_INDEX = "com.fsck.k9.EditIdentity_identity_index";
    public static final String EXTRA_ACCOUNT = "com.fsck.k9.EditIdentity_account";

    private Account mAccount;
    private Identity mIdentity;
    private EditText mDescriptionView;
    private CheckBox mSignatureUse;
    private EditText mSignatureView;
    private LinearLayout mSignatureLayout;
    private EditText mEmailView;
    private EditText mNameView;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(getApplication().getApplicationContext()).getAccount(accountUuid);
        String identityUuid = getIntent().getStringExtra(EXTRA_IDENTITY);
        
        if (identityUuid == null)
        {
            mIdentity = mAccount.newIdentity();
        }
        else
        {
            mIdentity = mAccount.getIdentity(identityUuid);
        }

        setContentView(R.layout.edit_identity);

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_IDENTITY))
        {
            identityUuid = savedInstanceState.getString(EXTRA_IDENTITY);
            mIdentity = mAccount.getIdentity(identityUuid);
        }

        mDescriptionView = (EditText)findViewById(R.id.description);
        mDescriptionView.setText(mIdentity.getDescription());

        mNameView = (EditText)findViewById(R.id.name);
        mNameView.setText(mIdentity.getName());

        mEmailView = (EditText)findViewById(R.id.email);
        mEmailView.setText(mIdentity.getEmail());

        mSignatureLayout = (LinearLayout)findViewById(R.id.signature_layout);
        mSignatureView = (EditText)findViewById(R.id.signature);
        mSignatureUse = (CheckBox)findViewById(R.id.signature_use);
        mSignatureUse.setChecked(mIdentity.getSignatureUse());
        mSignatureUse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    mSignatureLayout.setVisibility(View.VISIBLE);
                    mSignatureView.setText(mIdentity.getSignature());
                }
                else
                {
                    mSignatureLayout.setVisibility(View.GONE);
                }
            }
        });

        if (mSignatureUse.isChecked())
        {
            mSignatureView.setText(mIdentity.getSignature());
        }
        else
        {
            mSignatureLayout.setVisibility(View.GONE);
        }
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
        mIdentity.setName(mNameView.getText().toString());
        mIdentity.setSignatureUse(mSignatureUse.isChecked());
        mIdentity.setSignature(mSignatureView.getText().toString());
        mIdentity.save(Preferences.getPreferences(getApplication().getApplicationContext()));

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
        outState.putString(EXTRA_IDENTITY, mIdentity.getUuid());
    }
}
