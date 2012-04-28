package com.fsck.k9.activity.setup;

/*
    After gathering the necessary information one way or another this activity displays it to the user, eventually warns
    him it could be false ( no https ), asks for confirmation, allows selection of protocol,... and then goes on to test
    the final settings.
 */

import android.content.Context;
import android.content.Intent;
import com.fsck.k9.Account;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo;
import com.fsck.k9.helper.configxmlparser.AutoconfigInfo.ServerType;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.List;

public class AccountSetupConfirmIncoming extends AbstractSetupConfirmActivity {

    // account is allowed to be null
    public static void actionConfirmIncoming(Context context, Account account, String email, String password, AutoconfigInfo info, boolean makedefault) {
        Intent i = new Intent(context, AccountSetupConfirmIncoming.class);
        if( account != null )
            i.putExtra(EXTRA_ACCOUNT, account.getUuid());
        else i.putExtra(EXTRA_ACCOUNT, "");
        i.putExtra(EXTRA_EMAIL, email);
        i.putExtra(EXTRA_PASSWORD, password);
        i.putExtra(EXTRA_CONFIG_INFO, info);
        i.putExtra(EXTRA_MAKEDEFAULT, makedefault);
        context.startActivity(i);
    }

    @Override
    protected List<? extends AutoconfigInfo.Server> getServers() {
        return mConfigInfo.incomingServer;
    }

    @Override
    protected List<ServerType> getAvailableServerTypes() {
        return mConfigInfo.getAvailableIncomingServerTypes();
    }

    @Override
    protected void finishAction() {
        try {
            // this should already be set, only the username could be different.. but anyway
            String usernameEnc = URLEncoder.encode(mUsername, "UTF-8");
            String passwordEnc = URLEncoder.encode(mPassword, "UTF-8");

            URI uri = new URI(
                    getScheme(),
                    usernameEnc + ":" + passwordEnc,
                    mCurrentServer.hostname,
                    mCurrentServer.port,
                    null,null,null);
            mAccount.setStoreUri(uri.toString());

            AccountSetupConfirmOutgoing.actionConfirmOutgoing(this, mAccount, mEmail, mPassword, mConfigInfo, mMakeDefault);
        }
        catch (UnsupportedEncodingException enc) {}
        catch (URISyntaxException use) {
            /*
            * If we can't set up the URL we just continue. It's only for
            * convenience.
            */
        }
        finish();
    }
}
