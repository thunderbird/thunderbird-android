package com.fsck.k9.activity.setup;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.fsck.k9.preferences.Protocols;
import timber.log.Timber;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.ui.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.helper.EmailHelper;
import com.fsck.k9.setup.ServerNameSuggester;

import java.net.URI;
import java.net.URISyntaxException;


/**
 * Prompts the user to select an account type. The account type, along with the
 * passed in email address, password and makeDefault are then passed on to the
 * AccountSetupIncoming activity.
 */
public class AccountSetupAccountType extends K9Activity implements OnClickListener {
    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_MAKE_DEFAULT = "makeDefault";

    private final ServerNameSuggester serverNameSuggester = new ServerNameSuggester();
    private Account mAccount;
    private boolean mMakeDefault;

    public static void actionSelectAccountType(Context context, Account account, boolean makeDefault) {
        Intent i = new Intent(context, AccountSetupAccountType.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_MAKE_DEFAULT, makeDefault);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setLayout(R.layout.account_setup_account_type);
        findViewById(R.id.pop).setOnClickListener(this);
        findViewById(R.id.imap).setOnClickListener(this);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        mMakeDefault = getIntent().getBooleanExtra(EXTRA_MAKE_DEFAULT, false);
    }

    private void setupStoreAndSmtpTransport(String serverType, String schemePrefix) throws URISyntaxException {
        String domainPart = EmailHelper.getDomainFromEmailAddress(mAccount.getEmail());

        String suggestedStoreServerName = serverNameSuggester.suggestServerName(serverType, domainPart);
        URI storeUriForDecode = new URI(mAccount.getStoreUri());
        URI storeUri = new URI(schemePrefix, storeUriForDecode.getUserInfo(), suggestedStoreServerName,
                storeUriForDecode.getPort(), null, null, null);
        mAccount.setStoreUri(storeUri.toString());

        String suggestedTransportServerName = serverNameSuggester.suggestServerName(Protocols.SMTP, domainPart);
        URI transportUriForDecode = new URI(mAccount.getTransportUri());
        URI transportUri = new URI("smtp+tls+", transportUriForDecode.getUserInfo(), suggestedTransportServerName,
                transportUriForDecode.getPort(), null, null, null);
        mAccount.setTransportUri(transportUri.toString());
    }

    public void onClick(View v) {
        try {
            int id = v.getId();
            if (id == R.id.pop) {
                setupStoreAndSmtpTransport(Protocols.POP3, "pop3+ssl+");
            } else if (id == R.id.imap) {
                setupStoreAndSmtpTransport(Protocols.IMAP, "imap+ssl+");
            }
        } catch (Exception ex) {
            failure(ex);
        }

        AccountSetupIncoming.actionIncomingSettings(this, mAccount, mMakeDefault);
        finish();
    }

    private void failure(Exception use) {
        Timber.e(use, "Failure");
        String toastText = getString(R.string.account_setup_bad_uri, use.getMessage());

        Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_LONG);
        toast.show();
    }
}
