
package com.fsck.k9.external.remotecontrol;


import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.service.CoreReceiver;
import timber.log.Timber;

import static com.fsck.k9.external.remotecontrol.K9RemoteControl.K9_ACCOUNT_DESCRIPTIONS;
import static com.fsck.k9.external.remotecontrol.K9RemoteControl.K9_ACCOUNT_UUIDS;

public class RemoteControlReceiver extends CoreReceiver {
    @Override
    public Integer receive(Context context, Intent intent, Integer tmpWakeLockId) {
        Timber.i("RemoteControlReceiver.onReceive %s", intent);

        if (K9RemoteControl.K9_SET.equals(intent.getAction())) {
            RemoteControlService.set(context, intent, tmpWakeLockId);
            tmpWakeLockId = null;
        } else if (K9RemoteControl.K9_REQUEST_ACCOUNTS.equals(intent.getAction())) {
            try {
                Preferences preferences = Preferences.getPreferences(context);
                List<Account> accounts = preferences.getAccounts();
                String[] uuids = new String[accounts.size()];
                String[] descriptions = new String[accounts.size()];
                for (int i = 0; i < accounts.size(); i++) {
                    //warning: account may not be isAvailable()
                    Account account = accounts.get(i);

                    uuids[i] = account.getUuid();
                    descriptions[i] = account.getDescription();
                }
                Bundle bundle = getResultExtras(true);
                bundle.putStringArray(K9_ACCOUNT_UUIDS, uuids);
                bundle.putStringArray(K9_ACCOUNT_DESCRIPTIONS, descriptions);
            } catch (Exception e) {
                Timber.e(e, "Could not handle K9_RESPONSE_INTENT");
            }

        }

        return tmpWakeLockId;
    }

}
