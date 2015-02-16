
package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import java.net.URI;

/**
 * Prompts the user to select an account type. The account type, along with the
 * passed in email address, password and makeDefault are then passed on to the
 * AccountSetupIncoming activity.
 */
public class AccountSetupAccountType extends K9Activity implements OnClickListener {
    private static final String EXTRA_ACCOUNT = "account";

    private static final String EXTRA_MAKE_DEFAULT = "makeDefault";

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

    private void onPop() {
        try {
            URI uri = new URI(mAccount.getStoreUri());
            uri = new URI("pop3+ssl+", uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
            mAccount.setStoreUri(uri.toString());

            uri = new URI(mAccount.getTransportUri());
            uri = new URI("smtp+tls+", uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
            mAccount.setTransportUri(uri.toString());

            AccountSetupIncoming.actionIncomingSettings(this, mAccount, mMakeDefault);
            finish();
        } catch (Exception use) {
            failure(use);
        }

    }

    private void onImap() {
        try {
            URI uri = new URI(mAccount.getStoreUri());
            uri = new URI("imap+ssl+", uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
            mAccount.setStoreUri(uri.toString());

            uri = new URI(mAccount.getTransportUri());
            uri = new URI("smtp+tls+", uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
            mAccount.setTransportUri(uri.toString());

            AccountSetupIncoming.actionIncomingSettings(this, mAccount, mMakeDefault);
            finish();
        } catch (Exception use) {
            failure(use);
        }

    }

    private void onWebDav() {
        try {
            URI uri = new URI(mAccount.getStoreUri());

            /*
             * The user info we have been given from
             * AccountSetupBasics.onManualSetup() is encoded as an IMAP store
             * URI: AuthType:UserName:Password (no fields should be empty).
             * However, AuthType is not applicable to WebDAV nor to its store
             * URI. Re-encode without it, using just the UserName and Password.
             */
            String userPass = "";
            String[] userInfo = uri.getUserInfo().split(":");
            if (userInfo.length > 1) {
                userPass = userInfo[1];
            }
            if (userInfo.length > 2) {
                userPass = userPass + ":" + userInfo[2];
            }
            uri = new URI("webdav+ssl+", userPass, uri.getHost(), uri.getPort(), null, null, null);
            mAccount.setStoreUri(uri.toString());
            AccountSetupIncoming.actionIncomingSettings(this, mAccount, mMakeDefault);
            finish();
        } catch (Exception use) {
            failure(use);
        }

    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.pop:
            onPop();
            break;
        case R.id.imap:
            onImap();
            break;
        case R.id.webdav:
            onWebDav();
            break;
        }
    }

    private void failure(Exception use) {
        Log.e(K9.LOG_TAG, "Failure", use);
        String toastText = getString(R.string.account_setup_bad_uri, use.getMessage());

        Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_LONG);
        toast.show();
    }
}
