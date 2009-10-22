
package com.android.email.service;

import com.android.email.Email;

import android.net.ConnectivityManager;
import android.util.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
     	Log.v(Email.LOG_TAG, "BootReceiver.onReceive" + intent);

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
         	Email.setServicesEnabled(context);
        }
        else if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(intent.getAction())) {
            MailService.actionCancel(context);
        }
        else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(intent.getAction())) {
            MailService.actionReschedule(context);
        }
        else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            MailService.connectivityChange(context, !noConnectivity);
        }
    }
}
