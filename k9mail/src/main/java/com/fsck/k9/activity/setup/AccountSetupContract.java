package com.fsck.k9.activity.setup;


import java.net.URISyntaxException;
import java.security.cert.X509Certificate;

import android.content.Context;
import android.support.annotation.StringRes;

import com.fsck.k9.Account;
import com.fsck.k9.BasePresenter;
import com.fsck.k9.activity.setup.AccountSetupPresenter.Stage;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings.Type;


interface AccountSetupContract {
    interface View {
        // account type
        void goToIncomingSettings();

        // basics
        void setNextButtonInBasicsEnabled(boolean enabled);
        void goToAccountType();
        void goToAutoConfiguration();

        // check settings
        void showAcceptKeyDialog(final int msgResId,  final String exMessage, String message,
                X509Certificate certificate);
        void showErrorDialog(final int msgResId, final Object... args);
        boolean canceled();
        void setMessage(@StringRes int id);

        void goToBasics();
        void goToIncoming();
        void goToOutgoing();

        void goToOptions();

        Context getContext();

        /* --incoming-- */

        void goToIncomingChecking();
        void setNextButtonInIncomingEnabled(boolean enabled);
        void setAuthTypeInIncoming(AuthType authType);
        void setSecurityTypeInIncoming(ConnectionSecurity security);

        void setUsernameInIncoming(String username);
        void setPasswordInIncoming(String password);
        void setCertificateAliasInIncoming(String alias);
        void setServerInIncoming(String server);
        void setPortInIncoming(String port);

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

        void setViewNotExternalInIncoming();
        void setViewExternalInIncoming();

        void showFailureToast(Exception use);

        void setCompressionMobile(boolean compressionMobile);
        void setCompressionWifi(boolean compressionWifi);
        void setCompressionOther(boolean compressionOther);

        void setSubscribedFoldersOnly(boolean subscribedFoldersOnly);

        void showInvalidSettingsToast();

        /* --Names-- */
        void goToListAccounts();

        /* --options-- */
        void setNotifyViewChecked(boolean checked);
        void setNotifySyncViewChecked(boolean checked);
        void setCheckFrequencyViewValue(int value);
        void setDisplayCountViewValue(int value);
        void setPushEnableChecked(boolean checked);
        void setPushEnableVisibility(int visibility);
        void goToAccountNames();

        /* --outgoing-- */

        void setNextButtonInOutgoingEnabled(boolean enabled);

        void setAuthTypeInOutgoing(AuthType authType);
        void setSecurityTypeInOutgoing(ConnectionSecurity security);

        void setUsernameInOutgoing(String username);
        void setPasswordInOutgoing(String password);
        void setCertificateAliasInOutgoing(String alias);
        void setServerInOutgoing(String server);
        void setPortInOutgoing(String port);

        void showInvalidSettingsToastInOutgoing();

        void updateAuthPlainTextInOutgoing(boolean insecure);

        void setViewNotExternalInOutgoing();
        void setViewExternalInOutgoing();

        void goToOutgoingChecking();

        // ---
        void end();
    }

    interface Presenter extends BasePresenter {
        // account type
        void onAccountTypeStart();
        void onImapOrPop3Selected(Type serverType, String schemePrefix) throws URISyntaxException;
        void onWebdavSelected() throws URISyntaxException;

        // basics
        void onBasicsStart();
        void onInputChangedInBasics(String email, String password);
        void onManualSetupButtonClicked(String email, String password);
        void onNextButtonInBasicViewClicked(String email, String password);
        void setAccount(Account account);
        Account getAccount();

        /* checking */
        void onNegativeClickedInConfirmationDialog();

        void onCheckingStart(Stage stage);

        void onCertificateAccepted(X509Certificate certificate);
        void onPositiveClickedInConfirmationDialog();

        /* incoming */

        void onIncomingStart(boolean editSettings);

        void onIncomingStart();

        void onInputChangedInIncoming(String certificateAlias, String server, String port,
                                      String username, String password, AuthType authType,
                                      ConnectionSecurity connectionSecurity);

        void onNextInIncomingClicked(String username, String password, String clientCertificateAlias,
                boolean autoDetectNamespace, String imapPathPrefix, String webdavPathPrefix, String webdavAuthPath,
                String webdavMailboxPath, String host, int port, ConnectionSecurity connectionSecurity,
                AuthType authType, boolean compressMobile, boolean compressWifi, boolean compressOther,
                boolean subscribedFoldersOnly);

        /* --names--*/
        void onNamesStart();
        void onNextButtonInNamesClicked(String name, String description);

        /* --options-- */
        void onOptionsStart();
        void onNextButtonInOptionsClicked(boolean isNotifyViewChecked, boolean isNotifySyncViewClicked,
                int checkFrequencyViewSelectedValue, int displayCountViewSelectedValue,
                boolean isPushEnableClicked);

        // outgoing
        void onOutgoingStart();

        void onOutgoingStart(boolean editSettings);

        void onNextInOutgoingClicked(String username, String password, String clientCertificateAlias,
                String host, int port, ConnectionSecurity connectionSecurity,
                AuthType authType, boolean requireLogin);

        void onInputChangedInOutgoing(String certificateAlias, String server, String port,
                String username, String password, AuthType authType,
                ConnectionSecurity connectionSecurity, boolean requireLogin);

        void onCertificateRefused();

        // ---

        void onGetAccountUuid(String accountUuid);
        void onRestoreStart();
        void onRestoreEnd();
        void onMakeDefault();
    }
}
