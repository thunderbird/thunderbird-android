package com.fsck.k9.activity.setup;

/*
    After gathering the necessary information one way or another this activity displays it to the user, eventually warns
    him it could be false ( no https ), asks for confirmation, allows selection of protocol,... and then goes on to test
    the final settings.
 */

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.ServerType;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.AuthenticationType;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

public class AccountSetupConfirmOutgoing extends AbstractSetupConfirmActivity{

    // account is allowed to be null
    public static void actionConfirmOutgoing(Context context, Account account, String email, String password, AutoconfigInfo info) {
        Intent i = new Intent(context, AccountSetupConfirmOutgoing.class);
        i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        i.putExtra(EXTRA_EMAIL, email);
        i.putExtra(EXTRA_PASSWORD, password);
        i.putExtra(EXTRA_CONFIG_INFO, info);
        context.startActivity(i);
    }

    @Override
    protected List<? extends AutoconfigInfo.Server> getServers() {
        return mConfigInfo.outgoingServer;
    }

    @Override
    protected List<ServerType> getAvailableServerTypes() {
        return mConfigInfo.getAvailableOutgoingServerTypes();
    }

    @Override
    protected void finishAction() {
        try {
            String userInfo = null;

            // check if authentication is required
            if (mCurrentServer.authentication != AuthenticationType.none &&
                mCurrentServer.authentication != AuthenticationType.clientIPaddress )
            {
                userInfo = URLEncoder.encode(mUsername, "UTF-8") + ":" +
                           URLEncoder.encode(mPassword, "UTF-8") + ":" +
                           mCurrentServer.authentication.getAuthString();
            }

            URI uri = new URI(
                    getScheme(),
                    userInfo,
                    mCurrentServer.hostname,
                    mCurrentServer.port,
                    null,null,null);

            mAccount.setTransportUri(uri.toString());
            AccountSetupCheckSettings.actionCheckSettings(this, mAccount, true, true);

        } catch (UnsupportedEncodingException enc) {
            // This really shouldn't happen since the encoding is hardcoded to UTF-8
            Log.e(K9.LOG_TAG, "Couldn't urlencode username or password.", enc);
        } catch (Exception e) {
            /*
             * It's unrecoverable if we cannot create a URI from components that
             * we validated to be safe.
             */
            // TODO: handle this
            //failure(e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (Intent.ACTION_EDIT.equals(getIntent().getAction())) {
                mAccount.save(Preferences.getPreferences(this));
                finish();
            } else {
                // have to pop up to ask if it should be default account now
                // hard-coded now for test purposes
                AccountSetupOptions.actionOptions(this, mAccount, true);
                finish();
            }
        }
    }
}