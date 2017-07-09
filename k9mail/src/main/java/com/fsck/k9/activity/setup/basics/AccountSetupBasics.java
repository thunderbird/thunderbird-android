package com.fsck.k9.activity.setup.basics;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import com.fsck.k9.activity.K9MaterialActivity;
import com.fsck.k9.activity.setup.AccountSetupAccountType;
import com.fsck.k9.activity.setup.AccountSetupNames;
import com.fsck.k9.activity.setup.checksettings.AccountSetupCheckSettings;
import com.fsck.k9.activity.setup.basics.BasicsContract.Presenter;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;


public class AccountSetupBasics extends K9MaterialActivity
        implements BasicsContract.View, OnClickListener, TextWatcher {

    private final static String EXTRA_ACCOUNT = "com.fsck.k9.AccountSetupBasics.account";

    private Presenter presenter;
    private EditText emailView;
    private EditText passwordView;
    private TextView nextButton;
    private Button manualSetupButton;

    public static void actionNewAccount(Context context) {
        Intent i = new Intent(context, AccountSetupBasics.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_basics);
        emailView = (EditText)findViewById(R.id.account_email);
        passwordView = (EditText)findViewById(R.id.account_password);
        manualSetupButton = (Button) findViewById(R.id.manual_setup);
        nextButton = (TextView) findViewById(R.id.next);
        nextButton.setOnClickListener(this);
        manualSetupButton.setOnClickListener(this);

        presenter = new BasicsPresenter(this);
    }

    private void initializeViewListeners() {
        emailView.addTextChangedListener(this);
        passwordView.addTextChangedListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (presenter.getAccount() != null) {
            outState.putString(EXTRA_ACCOUNT, presenter.getAccount().getUuid());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
            String accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT);
            presenter.setAccount(Preferences.getPreferences(this).getAccount(accountUuid));
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        /*
         * We wait until now to initialize the listeners because we didn't want
         * the OnCheckedChangeListener active while the
         * mClientCertificateCheckBox state was being restored because it could
         * trigger the pop-up of a ClientCertificateSpinner.chooseCertificate()
         * dialog.
         */
        initializeViewListeners();
        presenter.validateFields(emailView.getText().toString(), passwordView.getText().toString());
    }

    public void afterTextChanged(Editable s) {
        presenter.validateFields(emailView.getText().toString(), passwordView.getText().toString());
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String email = emailView.getText().toString();
        String password = passwordView.getText().toString();
        presenter.handleAutoConfigurationResult(resultCode, email, password);
    }


    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.next:
                presenter.next();
                break;
            case R.id.manual_setup:
                String email = emailView.getText().toString();
                String password = passwordView.getText().toString();
                presenter.manualSetup(email, password);
                break;
        }
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void enableNext(boolean enabled) {
        nextButton.setEnabled(enabled);
    }

    @Override
    public void goToManualSetup(Account account) {
        AccountSetupAccountType.actionSelectAccountType(this, account, false);
    }

    @Override
    public void onAutoConfigurationSuccess(Account account) {
        AccountSetupNames.actionSetNames(this, account);
    }

    @Override
    public void goToAutoConfiguration(Account account) {
        String email = emailView.getText().toString();
        String password = passwordView.getText().toString();

        AccountSetupCheckSettings.startAutoConfigurationAndChecking(this, account, email, password);
    }
}
