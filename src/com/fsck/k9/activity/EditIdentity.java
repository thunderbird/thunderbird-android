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
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import java.util.List;

public class EditIdentity extends K9Activity
{

    public static final String EXTRA_IDENTITY = "com.fsck.k9.EditIdentity_identity";
    public static final String EXTRA_IDENTITY_INDEX = "com.fsck.k9.EditIdentity_identity_index";
    public static final String EXTRA_ACCOUNT = "com.fsck.k9.EditIdentity_account";

    private Account mAccount;
    private Identity mIdentity;
    private int mIdentityIndex;
    private EditText mDescriptionView;
    private CheckBox mSignatureUse;
    private EditText mSignatureView;
    private LinearLayout mSignatureLayout;
    private EditText mEmailView;
//  private EditText mAlwaysBccView;
    private EditText mNameView;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mIdentity = (Identity)getIntent().getSerializableExtra(EXTRA_IDENTITY);
        mIdentityIndex = getIntent().getIntExtra(EXTRA_IDENTITY_INDEX, -1);
        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);

        if (mIdentityIndex == -1)
        {
            mIdentity = new Identity();
        }

        setContentView(R.layout.edit_identity);

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_IDENTITY))
        {
            mIdentity = (Identity)savedInstanceState.getSerializable(EXTRA_IDENTITY);
        }

        mDescriptionView = (EditText)findViewById(R.id.description);
        mDescriptionView.setText(mIdentity.getDescription());

        mNameView = (EditText)findViewById(R.id.name);
        mNameView.setText(mIdentity.getName());

        mEmailView = (EditText)findViewById(R.id.email);
        mEmailView.setText(mIdentity.getEmail());

//      mAccountAlwaysBcc = (EditText)findViewById(R.id.bcc);
//      mAccountAlwaysBcc.setText(mIdentity.getAlwaysBcc());

        mSignatureLayout = (LinearLayout)findViewById(R.id.signature_layout);
        mSignatureUse = (CheckBox)findViewById(R.id.signature_use);
        mSignatureView = (EditText)findViewById(R.id.signature);
        mSignatureUse.setChecked(mIdentity.getSignatureUse());
        mSignatureUse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
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
        //      mIdentity.setAlwaysBcc(mAccountAlwaysBcc.getText().toString());
        mIdentity.setName(mNameView.getText().toString());
        mIdentity.setSignatureUse(mSignatureUse.isChecked());
        mIdentity.setSignature(mSignatureView.getText().toString());

        List<Identity> identities = mAccount.getIdentities();
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
