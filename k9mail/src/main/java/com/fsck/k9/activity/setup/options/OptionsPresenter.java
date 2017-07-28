package com.fsck.k9.activity.setup.options;

import com.fsck.k9.Account;
import com.fsck.k9.BasePresenter;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.setup.AccountSetupNames;
import com.fsck.k9.activity.setup.options.OptionsContract.View;
import com.fsck.k9.activity.setup.AbstractAccountSetup.AccountState;


class OptionsPresenter implements OptionsContract.Presenter {
    View view;
    AccountState state;

    OptionsPresenter(View view, AccountState state) {
        this.view = view;
        view.setPresenter(this);
        this.state = state;
    }

    @Override
    public void onNextButtonClicked(boolean isNotifyViewChecked, boolean isNotifySyncViewClicked,
                                    int checkFrequencyViewSelectedValue, int displayCountViewSelectedValue,
                                    boolean isPushEnableClicked) {
        Account account = state.getAccount();

        account.setDescription(account.getEmail());
        account.setNotifyNewMail(isNotifyViewChecked);
        account.setShowOngoing(isNotifySyncViewClicked);
        account.setAutomaticCheckIntervalMinutes(checkFrequencyViewSelectedValue);
        account.setDisplayCount(displayCountViewSelectedValue);

        if (isPushEnableClicked) {
            account.setFolderPushMode(Account.FolderMode.FIRST_CLASS);
        } else {
            account.setFolderPushMode(Account.FolderMode.NONE);
        }

        account.save(Preferences.getPreferences(K9.app));
        if (account.equals(Preferences.getPreferences(K9.app).getDefaultAccount()) ||
                state.isMakeDefault()) {
            Preferences.getPreferences(K9.app).setDefaultAccount(account);
        }
        K9.setServicesEnabled(K9.app);

        view.next();
        // AccountSetupNames.actionSetNames(this, account);
        // finish();
    }
}
