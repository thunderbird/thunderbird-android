package com.fsck.k9.activity.setup.basics;


import com.fsck.k9.Account;
import com.fsck.k9.BasePresenter;
import com.fsck.k9.BaseView;


interface BasicsContract {
    interface View extends BaseView<Presenter> {
        void setNextEnabled(boolean enabled);
        void goToManualSetup(Account account);
        void goToAutoConfiguration(Account account);
    }

    interface Presenter extends BasePresenter {
        void onInputChanged(String email, String password);
        void onManualSetupButtonClicked(String email, String password);
        void onNextButtonClicked(String email, String password);
        void setAccount(Account account);
        Account getAccount();
    }
}
