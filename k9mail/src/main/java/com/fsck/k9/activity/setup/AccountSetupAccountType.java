package com.fsck.k9.activity.setup;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import timber.log.Timber;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.helper.EmailHelper;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.setup.ServerNameSuggester;

import java.net.URI;
import java.net.URISyntaxException;

import static com.fsck.k9.mail.ServerSettings.Type.IMAP;
import static com.fsck.k9.mail.ServerSettings.Type.POP3;
import static com.fsck.k9.mail.ServerSettings.Type.SMTP;
import static com.fsck.k9.mail.ServerSettings.Type.WebDAV;


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
        setContentView(R.layout.account_setup_account_type);
        findViewById(R.id.pop).setOnClickListener(this);
        findViewById(R.id.imap).setOnClickListener(this);
        findViewById(R.id.webdav).setOnClickListener(this);

        String accountUuid = getIntent().getStringExtra(EXTRA_ACCOUNT);
        mAccount = Preferences.getPreferences(this).getAccount(accountUuid);
        mMakeDefault = getIntent().getBooleanExtra(EXTRA_MAKE_DEFAULT, false);
    }

    private void setupStoreAndSmtpTransport(Type serverType, String schemePrefix) throws URISyntaxException {
        String domainPart = EmailHelper.getDomainFromEmailAddress(mAccount.getEmail());

        String suggestedStoreServerName = serverNameSuggester.suggestServerName(serverType, domainPart);
        URI storeUriForDecode = new URI(mAccount.getStoreUri());
        URI storeUri = new URI(schemePrefix, storeUriForDecode.getUserInfo(), suggestedStoreServerName,
                storeUriForDecode.getPort(), null, null, null);
        mAccount.setStoreUri(storeUri.toString());

        String suggestedTransportServerName = serverNameSuggester.suggestServerName(SMTP, domainPart);
        URI transportUriForDecode = new URI(mAccount.getTransportUri());
        URI transportUri = new URI("smtp+tls+", transportUriForDecode.getUserInfo(), suggestedTransportServerName,
                transportUriForDecode.getPort(), null, null, null);
        mAccount.setTransportUri(transportUri.toString());
    }

    private void setupDav() throws URISyntaxException {
        URI uriForDecode = new URI(mAccount.getStoreUri());

        /*
         * The user info we have been given from
         * AccountSetupBasics.onManualSetup() is encoded as an IMAP store
         * URI: AuthType:UserName:Password (no fields should be empty).
         * However, AuthType is not applicable to WebDAV nor to its store
         * URI. Re-encode without it, using just the UserName and Password.
         */
        String userPass = "";
        String[] userInfo = uriForDecode.getUserInfo().split(":");
        if (userInfo.length > 1) {
            userPass = userInfo[1];
        }
        if (userInfo.length > 2) {
            userPass = userPass + ":" + userInfo[2];
        }

        String domainPart = EmailHelper.getDomainFromEmailAddress(mAccount.getEmail());
        String suggestedServerName = serverNameSuggester.suggestServerName(WebDAV, domainPart);
        URI uri = new URI("webdav+ssl+", userPass, suggestedServerName, uriForDecode.getPort(), null, null, null);
        mAccount.setStoreUri(uri.toString());
    }

    public void onClick(View v) {
        try {
            switch (v.getId()) {
                case R.id.pop: {
                    setupStoreAndSmtpTransport(POP3, "pop3+ssl+");
                    break;
                }
                case R.id.imap: {
                    setupStoreAndSmtpTransport(IMAP, "imap+ssl+");
                    break;
                }
                case R.id.webdav: {
                    setupDav();
                    break;
                }
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
