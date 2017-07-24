package com.fsck.k9.activity.setup.basics;


import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;

import com.fsck.k9.activity.setup.AbstractAccountSetup;
import com.fsck.k9.activity.setup.basics.BasicsContract.Presenter;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.R;


public class BasicsView implements BasicsContract.View, OnClickListener, TextWatcher {

    private final static String EXTRA_ACCOUNT = "com.fsck.k9.BasicsView.account";

    private AbstractAccountSetup activity;
    private Presenter presenter;
    private EditText emailView;
    private EditText passwordView;
    private TextView nextButton;
    private Button manualSetupButton;

    public static void actionNewAccount(Context context) {
        // FIXME: 7/24/2017 change it
        Intent i = new Intent(context, BasicsView.class);
        context.startActivity(i);
    }

    @Override
    public void setActivity(AbstractAccountSetup activity) {
        this.activity = activity;
    }

    @Override
    public void start() {
    }

    public BasicsView(AbstractAccountSetup activity) {
        setActivity(activity);

        emailView = (EditText) activity.findViewById(R.id.account_email);
        passwordView = (EditText) activity.findViewById(R.id.account_password);
        manualSetupButton = (Button) activity.findViewById(R.id.manual_setup);
        nextButton = (TextView) activity.findViewById(R.id.next);
        nextButton.setOnClickListener(this);
        manualSetupButton.setOnClickListener(this);

        presenter = new BasicsPresenter(this);

        initializeViewListeners();
    }

    private void initializeViewListeners() {
        emailView.addTextChangedListener(this);
        passwordView.addTextChangedListener(this);
    }

    /*
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

        initializeViewListeners();
        presenter.onInputChanged(emailView.getText().toString(), passwordView.getText().toString());
    } */

    public void afterTextChanged(Editable s) {
        presenter.onInputChanged(emailView.getText().toString(), passwordView.getText().toString());
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    // FIXME: 7/23/2017 update it
    /* @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String email = emailView.getText().toString();
        String password = passwordView.getText().toString();
        presenter.onAutoConfigurationResult(resultCode, email, password);
    } */


    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.next:
                presenter.onNextButtonClicked();
                break;
            case R.id.manual_setup:
                String email = emailView.getText().toString();
                String password = passwordView.getText().toString();
                presenter.onManualSetupButtonClicked(email, password);
                break;
        }
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setNextEnabled(boolean enabled) {
        nextButton.setEnabled(enabled);
    }

    @Override
    public void goToManualSetup(Account account) {
        activity.goToManualSetup();
        // AccountSetupAccountType.actionSelectAccountType(this, account, false);
    }

    @Override
    public void onAutoConfigurationSuccess(Account account) {
        activity.goToAccountNames();
        // AccountSetupNames.actionSetNames(this, account);
    }

    @Override
    public void goToAutoConfiguration(Account account) {
        activity.goToAutoConfiguration();
        // String email = emailView.getText().toString();
        // String password = passwordView.getText().toString();

        // CheckSettingsView.startAutoConfigurationAndChecking(this, account, email, password);
    }
}
