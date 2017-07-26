package com.fsck.k9.activity.setup.basics;


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

    private AbstractAccountSetup activity;
    private Presenter presenter;
    private EditText emailView;
    private EditText passwordView;
    private TextView nextButton;
    private Button manualSetupButton;

    @Override
    public void setActivity(AbstractAccountSetup activity) {
        this.activity = activity;
    }

    @Override
    public void start() {
        emailView = (EditText) activity.findViewById(R.id.account_email);
        passwordView = (EditText) activity.findViewById(R.id.account_password);
        manualSetupButton = (Button) activity.findViewById(R.id.manual_setup);
        nextButton = (TextView) activity.findViewById(R.id.next);
        nextButton.setOnClickListener(this);
        manualSetupButton.setOnClickListener(this);

        presenter = new BasicsPresenter(this, activity.getState());

        initializeViewListeners();
    }

    public BasicsView(AbstractAccountSetup activity) {
        setActivity(activity);

    }

    private void initializeViewListeners() {
        emailView.addTextChangedListener(this);
        passwordView.addTextChangedListener(this);
    }

    public void afterTextChanged(Editable s) {
        presenter.onInputChanged(emailView.getText().toString(), passwordView.getText().toString());
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void onClick(View v) {
        String email = emailView.getText().toString();
        String password = passwordView.getText().toString();
        switch (v.getId()) {
            case R.id.next:
                presenter.onNextButtonClicked(email, password);
                break;
            case R.id.manual_setup:
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
    }

    @Override
    public void goToAutoConfiguration(Account account) {
        activity.goToAutoConfiguration();
    }
}
