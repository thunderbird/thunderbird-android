package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.KeyEvent;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.EditTextPreference;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.k9;
import com.fsck.k9.Utility;

public class AccountSetupComposition extends Activity {

    private static final String EXTRA_ACCOUNT = "account";
    // rivate static final String EXTRA_MAKE_DEFAULT = "makeDefault";

    private static final String PREFERENCE_ALWAYS_BCC = "account_always_bcc";
    private static final String PREFERENCE_EMAIL = "account_email";
    private static final String PREFERENCE_SIGNATURE = "account_signature";

    private Account mAccount;

    private EditText mAccountSignature;
    private EditText mAccountEmail;
    private EditText mAccountAlwaysBcc;




     public static void actionCompositionSettings(Activity context, Account account, boolean makeDefault) {
        Intent i = new Intent(context, AccountSetupComposition.class);
        i.putExtra(EXTRA_ACCOUNT, account);
        //i.putExtra(EXTRA_MAKE_DEFAULT, makeDefault);
        context.startActivity(i);
    }


    public static void actionEditCompositionSettings(Activity context, Account account) {
        Intent i = new Intent(context, AccountSetupComposition.class);
        i.setAction(Intent.ACTION_EDIT);
        i.putExtra(EXTRA_ACCOUNT, account);
        context.startActivity(i);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAccount = (Account)getIntent().getSerializableExtra(EXTRA_ACCOUNT);
        // addPreferencesFromResource(R.xml.account_settings_preferences);


        setContentView(R.layout.account_setup_composition);

        /*
         * If we're being reloaded we override the original account with the one
         * we saved
         */
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            mAccount = (Account)savedInstanceState.getSerializable(EXTRA_ACCOUNT);
        }

        mAccountEmail = (EditText)findViewById(R.id.account_email);
        mAccountEmail.setText(mAccount.getEmail());

        mAccountAlwaysBcc = (EditText)findViewById(R.id.account_always_bcc);
        mAccountAlwaysBcc.setText(mAccount.getAlwaysBcc());

        mAccountSignature = (EditText)findViewById(R.id.account_signature);
        mAccountSignature.setText(mAccount.getSignature());

    }

    @Override
    public void onResume() {
        super.onResume();
        mAccount.refresh(Preferences.getPreferences(this));
    }

    private void saveSettings() {
        mAccount.setEmail(mAccountEmail.getText().toString());
        mAccount.setAlwaysBcc(mAccountAlwaysBcc.getText().toString()); 
       mAccount.setSignature(mAccountSignature.getText().toString()); 
        mAccount.save(Preferences.getPreferences(this));
        k9.setServicesEnabled(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            saveSettings();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(EXTRA_ACCOUNT, mAccount);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
                mAccount.save(Preferences.getPreferences(this));
                finish();
    }
}
