package com.fsck.k9.activity.setup.names;


import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.setup.AbstractAccountSetup.AccountState;
import com.fsck.k9.activity.setup.names.NamesContract.View;
import com.fsck.k9.helper.Utility;


public class NamesPresenter implements NamesContract.Presenter {
    View view;
    AccountState state;

    public NamesPresenter(View view, AccountState state) {
        this.view = view;
        this.state = state;
    }

    @Override
    public void onNextButtonClicked(String name, String description) {
        Account account = state.getAccount();

        if (Utility.requiredFieldValid(description)) {
            account.setDescription(description);
        }

        account.setName(name);
        account.save(Preferences.getPreferences(K9.app));

        view.onSetupFinished();
    }
}
