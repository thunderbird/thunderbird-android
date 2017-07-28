package com.fsck.k9.activity.setup.incoming;


import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.BasePresenter;
import com.fsck.k9.BaseView;
import com.fsck.k9.activity.setup.IncomingAndOutgoingState;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;


interface IncomingContract {
    interface View extends BaseView<Presenter> {
        void goToOutgoingSettings(Account account);
        void setNextEnabled(boolean enabled);
        Context getContext();
        void setAuthType(AuthType authType);
        void setSecurityType(ConnectionSecurity security);

        void setUsername(String username);
        void setPassword(String password);
        void setCertificateAlias(String alias);
        void setServer(String server);
        void setPort(String port);

        void setServerLabel(String label);

        void hideViewsWhenPop3();
        void hideViewsWhenImap();
        void hideViewsWhenImapAndNotEdit();
        void hideViewsWhenWebDav();

        void setImapAutoDetectNamespace(boolean autoDetectNamespace);
        void setImapPathPrefix(String prefix);

        void setWebDavPathPrefix(String prefix);
        void setWebDavAuthPath(String authPath);
        void setWebDavMailboxPath(String mailboxPath);

        void setSecurityChoices(ConnectionSecurity[] choices);

        void setAuthTypeInsecureText(boolean insecure);

        void onAuthTypeIsNotExternal();
        void onAuthTypeIsExternal();

        void onAccountLoadFailure(Exception use);

        void setCompressionMobile(boolean compressionMobile);
        void setCompressionWifi(boolean compressionWifi);
        void setCompressionOther(boolean compressionOther);

        void setSubscribedFoldersOnly(boolean subscribedFoldersOnly);

        void showInvalidSettingsToast();

    }

    interface Presenter extends BasePresenter {
        void onInputChanged(String certificateAlias, String server, String port,
                            String username, String password, AuthType authType,
                            ConnectionSecurity connectionSecurity);

        void modifyAccount(String username, String password, String clientCertificateAlias,
                boolean autoDetectNamespace, String imapPathPrefix, String webdavPathPrefix, String webdavAuthPath,
                String webdavMailboxPath, String host, int port, ConnectionSecurity connectionSecurity,
                AuthType authType, boolean compressMobile, boolean compressWifi, boolean compressOther,
                boolean subscribedFoldersOnly);

        void prepareForOutgoing(String username, String password, String certificateAlias, AuthType authType);
        void updateAccount();
        Account getAccount();
        IncomingAndOutgoingState getState();
        void setState(IncomingAndOutgoingState state);
    }
}
