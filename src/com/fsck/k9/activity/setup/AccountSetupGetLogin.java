
package com.fsck.k9.activity.setup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import com.fsck.k9.*;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.helper.Utility;


/**
 * Prompts the user for the email address and password. Also prompts for
 * "Use this account as default" if this is the 2nd+ account being set up.
 */
public class AccountSetupGetLogin extends K9Activity
    implements OnClickListener, TextWatcher {

	public static void startForResult(Activity act) {
		act.startActivityForResult(new Intent(act,AccountSetupGetLogin.class), AccountSetupIndex.GET_LOGIN);
	}
	
    private EditText mEmailView;
    private EditText mPasswordView;
    //private CheckBox mDefaultView;
    private Button mNextButton;
    private Button mManualSetupButton;

    private EmailAddressValidator mEmailValidator = new EmailAddressValidator();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_basics);

        mEmailView = (EditText)findViewById(R.id.account_email);
        mPasswordView = (EditText)findViewById(R.id.account_password);
        //mDefaultView = (CheckBox)findViewById(R.id.account_default);
        mNextButton = (Button)findViewById(R.id.next);
        mManualSetupButton = (Button)findViewById(R.id.manual_setup);

        mNextButton.setOnClickListener(this);
        mManualSetupButton.setOnClickListener(this);

        mEmailView.addTextChangedListener(this);
        mPasswordView.addTextChangedListener(this);

        /*if (mPrefs.getAccounts().length > 0) {
            mDefaultView.setVisibility(View.VISIBLE);
        }*/
    }

    @Override
    public void onResume() {
        super.onResume();
        validateFields();
    }

    public void afterTextChanged(Editable s) {
        validateFields();
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    private void validateFields() {
        String email = mEmailView.getText().toString();
        boolean valid = Utility.requiredFieldValid(mEmailView)
                        && Utility.requiredFieldValid(mPasswordView)
                        && mEmailValidator.isValidAddressOnly(email);

        mNextButton.setEnabled(valid);
        mManualSetupButton.setEnabled(valid);
        /*
         * Dim the next button's icon to 50% if the button is disabled.
         * TODO this can probably be done with a stateful drawable. Check into it.
         * android:state_enabled
         */
        Utility.setCompoundDrawablesAlpha(mNextButton, mNextButton.isEnabled() ? 255 : 128);
    }

    public void onClick(View v) {
    	Intent data = new Intent();
    	data.putExtra(AccountSetupIndex.DATA_LOGIN, mEmailView.getText().toString());
    	data.putExtra(AccountSetupIndex.DATA_PASSWORD, mPasswordView.getText().toString());
    	//data.putExtra(AccountSetupIndex.DATA_DEFAULT, mDefaultView.isChecked());
        switch (v.getId()) {
        case R.id.next:
        	data.putExtra(AccountSetupIndex.DATA_MANUAL, false);
        	setResult(RESULT_OK, data);
        	finish();
            break;
        case R.id.manual_setup:
        	data.putExtra(AccountSetupIndex.DATA_MANUAL, true);
        	setResult(RESULT_OK, data);
        	finish();
            break;
        }
    }
}