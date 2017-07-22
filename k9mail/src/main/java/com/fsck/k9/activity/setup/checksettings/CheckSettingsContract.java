package com.fsck.k9.activity.setup.checksettings;


import java.security.cert.X509Certificate;

import android.support.annotation.StringRes;

import com.fsck.k9.Account;
import com.fsck.k9.BasePresenter;
import com.fsck.k9.BaseView;


interface CheckSettingsContract {
    interface View extends BaseView<Presenter> {
        void goNext(Account account);
        void autoConfigurationFail();
        void showAcceptKeyDialog(final int msgResId,  final String exMessage, String message,
                X509Certificate certificate);
        void showErrorDialog(final int msgResId, final Object... args);
        boolean canceled();
        void setMessage(@StringRes int id);
    }

    interface Presenter extends BasePresenter {
        void skip();
        void autoConfiguration(String email, String password);
        void checkSettings();
        void onCertificateAccepted(X509Certificate certificate);
    }
}
