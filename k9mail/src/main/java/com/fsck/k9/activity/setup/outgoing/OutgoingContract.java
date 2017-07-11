package com.fsck.k9.activity.setup.outgoing;


import com.fsck.k9.Account;
import com.fsck.k9.BasePresenter;
import com.fsck.k9.BaseState;
import com.fsck.k9.BaseView;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;


interface OutgoingContract {
    interface View extends BaseView<Presenter> {
        void setNextButtonEnabled(boolean enabled);

        void next(String accountUuid);

        void setAuthType(AuthType authType);
        void setSecurityType(ConnectionSecurity security);

        void setUsername(String username);
        void setPassword(String password);
        void setCertificateAlias(String alias);
        void setServer(String server);
        void setPort(String port);

        void showInvalidSettingsToast();

        void updateAuthPlainText(boolean insecure);

        void onAuthTypeIsNotExternal();
        void onAuthTypeIsExternal();

        void onAccountLoadFailure(Exception use);
    }

    interface Presenter extends BasePresenter {
        void onAccountEdited();
        Account getAccount();
        void setAccount(String accountUuid);

        void onNext(String username, String password, String clientCertificateAlias,
                    String host, int port, ConnectionSecurity connectionSecurity,
                    AuthType authType, boolean requireLogin);

        void onInputChanged(String certificateAlias, String server, String port,
                String username, String password, AuthType authType, ConnectionSecurity connectionSecurity,
                boolean requireLogin);

        OutgoingState getState();
        void setState(OutgoingState state);
    }

}
