package com.fsck.k9.activity.setup.accounttype;


import java.net.URISyntaxException;

import com.fsck.k9.BasePresenter;
import com.fsck.k9.BaseView;
import com.fsck.k9.mail.ServerSettings.Type;


class AccountTypeContract {
    interface View extends BaseView<Presenter> {
        void onSetupFinished();
    }

    interface Presenter extends BasePresenter {
        void setupStoreAndSmtpTransport(Type serverType, String schemePrefix) throws URISyntaxException;
        void setupDav() throws URISyntaxException;
    }
}
