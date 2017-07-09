package com.fsck.k9.activity.setup.outgoing;


import com.fsck.k9.Account;
import com.fsck.k9.BasePresenter;
import com.fsck.k9.BaseView;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;


public interface OutgoingContract {
    interface View extends BaseView<Presenter> {

        void enableNext(boolean enabled);

        void resetPreviousSettings();

        void saveSettings();

        void checkAccount(Account account);
    }

    interface Presenter extends BasePresenter {
        void updateAccount();
        Account getAccount();
        void setAccount(Account account);
        void checkWebdav();
        void next(String username, String password, String clientCertificateAlias,
                String host, int port, ConnectionSecurity connectionSecurity,
                AuthType authType, boolean requireLogin);

        void validateFields(String certificateAlias, String server, String port,
                String username, String password, AuthType authType, ConnectionSecurity connectionSecurity,
                boolean requireLogin);

        void revokeInvalidSettings(AuthType authType, ConnectionSecurity connectionSecurity);
    }
}
