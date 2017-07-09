package com.fsck.k9.activity.setup.basics;


import android.content.Intent;

import com.fsck.k9.Account;
import com.fsck.k9.BasePresenter;
import com.fsck.k9.BaseView;


/**
 * Created by daquexian on 6/26/17.
 */

public interface BasicsContract {
    interface View extends BaseView<Presenter> {
        void enableNext(boolean enabled);
        void goToManualSetup(Account account);
        void onAutoConfigurationSuccess(Account account);
        void goToAutoConfiguration(Account account);
    }

    interface Presenter extends BasePresenter {
        void validateFields(String email, String password);
        void manualSetup(String email, String password);
        void handleAutoConfigurationResult(int resultCode, String email, String password);
        void next();
        void setAccount(Account account);
        Account getAccount();
    }
}
