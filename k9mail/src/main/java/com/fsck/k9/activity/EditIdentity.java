package com.fsck.k9.activity;

import android.os.Bundle;
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

public class EditIdentity extends K9Activity {

    public static final String EXTRA_IDENTITY = "com.fsck.k9.EditIdentity_identity";
    public static final String EXTRA_IDENTITY_INDEX = "com.fsck.k9.EditIdentity_identity_index";
    public static final String EXTRA_ACCOUNT = "com.fsck.k9.EditIdentity_account";

    private Account account;
    private Identity identity;
    private int identityIndex;
    private EditText descriptionView;
    private CheckBox signatureUse;
    private EditText signatureView;
    private LinearLayout signatureLayout;
    private EditText emailView;
//  private EditText mAlwaysBccView;
    private EditText nameView;
    private EditText replyTo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        identity = (Identity)getIntent().getSerializableExtra(EXTRA_IDENTITY);
        identityIndex = getIntent().getIntExtra(EXTRA_IDENTITY_INDEX, -1);
        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        account = Preferences.getPreferences(this).getAccount(accountUuid);

        if (identityIndex == -1) {
            identity = new Identity();
        }

        setContentView(R.layout.edit_identity);

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_IDENTITY)) {
            identity = (Identity)savedInstanceState.getSerializable(EXTRA_IDENTITY);
        }

        descriptionView = (EditText)findViewById(R.id.description);
        descriptionView.setText(identity.getDescription());

        nameView = (EditText)findViewById(R.id.name);
        nameView.setText(identity.getName());

        emailView = (EditText)findViewById(R.id.email);
        emailView.setText(identity.getEmail());

        replyTo = (EditText) findViewById(R.id.reply_to);
        replyTo.setText(identity.getReplyTo());

//      mAccountAlwaysBcc = (EditText)findViewById(R.id.bcc);
//      mAccountAlwaysBcc.setText(identity.getAlwaysBcc());

        signatureLayout = (LinearLayout)findViewById(R.id.signature_layout);
        signatureUse = (CheckBox)findViewById(R.id.signature_use);
        signatureView = (EditText)findViewById(R.id.signature);
        signatureUse.setChecked(identity.getSignatureUse());
        signatureUse.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    signatureLayout.setVisibility(View.VISIBLE);
                    signatureView.setText(identity.getSignature());
                } else {
                    signatureLayout.setVisibility(View.GONE);
                }
            }
        });

        if (signatureUse.isChecked()) {
            signatureView.setText(identity.getSignature());
        } else {
            signatureLayout.setVisibility(View.GONE);
        }
    }

    private void saveIdentity() {

        identity.setDescription(descriptionView.getText().toString());
        identity.setEmail(emailView.getText().toString());
        //      identity.setAlwaysBcc(mAccountAlwaysBcc.getText().toString());
        identity.setName(nameView.getText().toString());
        identity.setSignatureUse(signatureUse.isChecked());
        identity.setSignature(signatureView.getText().toString());

        if (replyTo.getText().length() == 0) {
            identity.setReplyTo(null);
        } else {
            identity.setReplyTo(replyTo.getText().toString());
        }

        List<Identity> identities = account.getIdentities();
        if (identityIndex == -1) {
            identities.add(identity);
        } else {
            identities.remove(identityIndex);
            identities.add(identityIndex, identity);
        }

        account.save(Preferences.getPreferences(getApplication().getApplicationContext()));

        finish();
    }

    @Override
    public void onBackPressed() {
        saveIdentity();
        super.onBackPressed();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_IDENTITY, identity);
    }
}
