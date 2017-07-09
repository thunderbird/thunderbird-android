package com.fsck.k9.activity.setup.basics;


import com.fsck.k9.Account;
import com.fsck.k9.EmailAddressValidator;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.setup.basics.BasicsContract.View;
import com.fsck.k9.helper.EmailHelper;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.store.RemoteStore;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;


class BasicsPresenter implements BasicsContract.Presenter {
    private View view;
    private Account account;

    BasicsPresenter(View view) {
        this.view = view;
        view.setPresenter(this);
    }

    @Override
    public void validateFields(String email, String password) {
        EmailAddressValidator emailValidator = new EmailAddressValidator();

        boolean valid = email != null && email.length() > 0
                && password != null && password.length() > 0
                && emailValidator.isValidAddressOnly(email);

        view.enableNext(valid);
    }

    @Override
    public void manualSetup(String email, String password) {
        if (account == null) {
            account = Preferences.getPreferences(K9.app).newAccount();
        }
        account.setName(getOwnerName());
        account.setEmail(email);

        EmailHelper emailHelper = new EmailHelper();
        String[] emailParts = emailHelper.splitEmail(email);
        String user = emailParts[0];
        String domain = emailParts[1];

        // set default uris
        // NOTE: they will be changed again in AccountSetupAccountType!
        ServerSettings storeServer = new ServerSettings(ServerSettings.Type.IMAP, "mail." + domain, -1,
                ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.PLAIN, user, password, null);
        ServerSettings transportServer = new ServerSettings(ServerSettings.Type.SMTP, "mail." + domain, -1,
                ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.PLAIN, user, password, null);
        String storeUri = RemoteStore.createStoreUri(storeServer);
        String transportUri = Transport.createTransportUri(transportServer);
        account.setStoreUri(storeUri);
        account.setTransportUri(transportUri);

        setupFolderNames(account, domain);

        view.goToManualSetup(account);
    }

    @Override
    public void handleAutoConfigurationResult(int resultCode, String email, String password) {
        if (resultCode == RESULT_OK) {
            view.onAutoConfigurationSuccess(account);
        } else {
            manualSetup(email, password);
        }
    }

    @Override
    public void next() {
        if (account == null) {
            account = Preferences.getPreferences(K9.app).newAccount();
        }
        view.goToAutoConfiguration(account);
    }

    @Override
    public void setAccount(Account account) {
        this.account = account;
    }

    @Override
    public Account getAccount() {
        return account;
    }

    private void setupFolderNames(Account account, String domain) {
        account.setDraftsFolderName(K9.getK9String(R.string.special_mailbox_name_drafts));
        account.setTrashFolderName(K9.getK9String(R.string.special_mailbox_name_trash));
        account.setSentFolderName(K9.getK9String(R.string.special_mailbox_name_sent));
        account.setArchiveFolderName(K9.getK9String(R.string.special_mailbox_name_archive));

        // Yahoo! has a special folder for Spam, called "Bulk Mail".
        if (domain.endsWith(".yahoo.com")) {
            account.setSpamFolderName("Bulk Mail");
        } else {
            account.setSpamFolderName(K9.getK9String(R.string.special_mailbox_name_spam));
        }
    }


    private String getOwnerName() {
        String name = null;
        try {
            name = getDefaultAccountName();
        } catch (Exception e) {
            Timber.e(e, "Could not get default account name");
        }

        if (name == null) {
            name = "";
        }
        return name;
    }

    private String getDefaultAccountName() {
        String name = null;
        Account account = Preferences.getPreferences(K9.app).getDefaultAccount();
        if (account != null) {
            name = account.getName();
        }
        return name;
    }
}
