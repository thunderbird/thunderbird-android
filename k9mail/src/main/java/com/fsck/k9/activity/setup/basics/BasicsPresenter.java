package com.fsck.k9.activity.setup.basics;


import com.fsck.k9.Account;
import com.fsck.k9.EmailAddressValidator;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.setup.AbstractAccountSetup.AccountState;
import com.fsck.k9.activity.setup.basics.BasicsContract.View;
import com.fsck.k9.helper.EmailHelper;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.store.RemoteStore;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;


class BasicsPresenter implements BasicsContract.Presenter {
    private View view;
    private Account account;
    private AccountState state;

    BasicsPresenter(View view, AccountState state) {
        this.view = view;
        view.setPresenter(this);
        this.state = state;
    }

    @Override
    public void onInputChanged(String email, String password) {
        EmailAddressValidator emailValidator = new EmailAddressValidator();

        boolean valid = email != null && email.length() > 0
                && password != null && password.length() > 0
                && emailValidator.isValidAddressOnly(email);

        view.setNextEnabled(valid);
    }

    @Override
    public void onManualSetupButtonClicked(String email, String password) {
        if (account == null) {
            account = Preferences.getPreferences(K9.app).newAccount();
        }

        account.init(email, password);

        state.setAccount(account);
        state.setEmail(email);
        state.setPassword(password);

        view.goToManualSetup(account);
    }

    @Override
    public void onAutoConfigurationResult(int resultCode, String email, String password) {
        if (resultCode == RESULT_OK) {
            view.onAutoConfigurationSuccess(account);
        } else {
            onManualSetupButtonClicked(email, password);
        }
    }

    @Override
    public void onNextButtonClicked(String email, String password) {
        if (account == null) {
            account = Preferences.getPreferences(K9.app).newAccount();
        }
        account.setEmail(email);

        state.setEmail(email);
        state.setPassword(password);

        state.setAccount(account);
        view.goToAutoConfiguration(account);
    }

    @Override
    public void setAccount(Account account) {
        this.account = account;
    }

    @Override
    public Account getAccount() {
        return account;
    }

}
