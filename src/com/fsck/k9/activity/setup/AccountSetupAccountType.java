
package com.fsck.k9.activity.setup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9Activity;
import com.fsck.k9.R;

import java.net.URI;

/**
 * Prompts the user to select an account type. The account type, along with the
 * passed in email address, password and makeDefault are then passed on to the
 * AccountSetupIncoming activity.
 */
public class AccountSetupAccountType extends K9Activity implements OnClickListener
{
    private static final String EXTRA_ACCOUNT = "account";

    private static final String EXTRA_MAKE_DEFAULT = "makeDefault";

    private Account mAccount;

    private boolean mMakeDefault;

    public static void actionSelectAccountType(Context context, Account account, boolean makeDefault)
    {
        Intent i = new Intent(context, AccountSetupAccountType.class);
        i.putExtra(EXTRA_ACCOUNT, account);
        i.putExtra(EXTRA_MAKE_DEFAULT, makeDefault);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_setup_account_type);
        ((Button)findViewById(R.id.pop)).setOnClickListener(this);
        ((Button)findViewById(R.id.imap)).setOnClickListener(this);
        ((Button)findViewById(R.id.webdav)).setOnClickListener(this);

        mAccount = (Account)getIntent().getSerializableExtra(EXTRA_ACCOUNT);
        mMakeDefault = (boolean)getIntent().getBooleanExtra(EXTRA_MAKE_DEFAULT, false);
    }

    private void onPop()
    {
        try
        {
            URI uri = new URI(mAccount.getStoreUri());
            uri = new URI("pop3", uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
            mAccount.setStoreUri(uri.toString());
            AccountSetupIncoming.actionIncomingSettings(this, mAccount, mMakeDefault);
            finish();
        }
        catch (Exception use)
        {
            failure(use);
        }

    }

    private void onImap()
    {
        try
        {
            URI uri = new URI(mAccount.getStoreUri());
            uri = new URI("imap", uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
            mAccount.setStoreUri(uri.toString());
            AccountSetupIncoming.actionIncomingSettings(this, mAccount, mMakeDefault);
            finish();
        }
        catch (Exception use)
        {
            failure(use);
        }

    }

    private void onWebDav()
    {
        try
        {
            URI uri = new URI(mAccount.getStoreUri());
            uri = new URI("webdav", uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
            mAccount.setStoreUri(uri.toString());
            AccountSetupIncoming.actionIncomingSettings(this, mAccount, mMakeDefault);
            finish();
        }
        catch (Exception use)
        {
            failure(use);
        }

    }

    public void onClick(View v)
    {
        switch (v.getId())
        {
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
    private void failure(Exception use)
    {
        Log.e(K9.LOG_TAG, "Failure", use);
        String toastText = getString(R.string.account_setup_bad_uri, use.getMessage());

        Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_LONG);
        toast.show();
    }
}
