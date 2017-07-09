package com.fsck.k9.activity.setup.incoming;


import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.BasePresenter;
import com.fsck.k9.BaseView;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;


public interface IncomingContract {
    interface View extends BaseView<Presenter> {
        void goToOutgoingSettings(Account account);
        void resetPreviousSettings();
        void saveSettings();
        void enableNext(boolean enabled);
        Context getContext();
    }

    interface Presenter extends BasePresenter {
        void modifyAccount(String username, String password, String clientCertificateAlias,
                boolean autoDetectNamespace, String imapPathPrefix, String webdavPathPrefix, String webdavAuthPath,
                String webdavMailboxPath, String host, int port, ConnectionSecurity connectionSecurity,
                AuthType authType, boolean compressMobile, boolean compressWifi, boolean compressOther,
                boolean subscribedFoldersOnly);

        void revokeInvalidSettings(AuthType authType, ConnectionSecurity connectionSecurity);
        void validateFields(String certificateAlias, String server, String port,
                String username, String password, AuthType authType, ConnectionSecurity connectionSecurity);
        void prepareForOutgoing(String username, String password, String certificateAlias, AuthType authType);
        void updateAccount();
        Account getAccount();
    }
}
