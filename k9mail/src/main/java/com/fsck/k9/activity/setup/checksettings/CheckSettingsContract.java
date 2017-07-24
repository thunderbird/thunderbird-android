package com.fsck.k9.activity.setup.checksettings;


import java.security.cert.X509Certificate;

import android.support.annotation.StringRes;

import com.fsck.k9.Account;
import com.fsck.k9.BasePresenter;
import com.fsck.k9.BaseView;
import com.fsck.k9.activity.setup.AbstractAccountSetup;


interface CheckSettingsContract {
    interface View extends BaseView<Presenter> {
        void autoConfigurationFail();
        void showAcceptKeyDialog(final int msgResId,  final String exMessage, String message,
                X509Certificate certificate);
        void showErrorDialog(final int msgResId, final Object... args);
        boolean canceled();
        void setMessage(@StringRes int id);

        void goToBasics();
        void goToIncoming();
        void goToOutgoing();
        void goToNames();
    }

    interface Presenter extends BasePresenter {
        void skip();
        void onViewStart(AbstractAccountSetup.AccountState state);
        void onCertificateAccepted(X509Certificate certificate);
        void onError();
    }
}
