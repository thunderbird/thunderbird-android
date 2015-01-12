package com.fsck.k9.remotecontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

class AccountReceiver extends BroadcastReceiver {
    K9AccountReceptor receptor = null;

    protected AccountReceiver(K9AccountReceptor nReceptor) {
        receptor = nReceptor;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (K9RemoteControl.K9_REQUEST_ACCOUNTS.equals(intent.getAction())) {
            Bundle bundle = getResultExtras(false);
            if (bundle == null) {
                Log.w(K9RemoteControl.LOG_TAG, "Response bundle is empty");
                return;
            }
            receptor.accounts(bundle.getStringArray(K9RemoteControl.K9_ACCOUNT_UUIDS), bundle.getStringArray(K9RemoteControl.K9_ACCOUNT_DESCRIPTIONS));
        }
    }

}
