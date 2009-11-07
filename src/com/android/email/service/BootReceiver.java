
package com.android.email.service;

import com.android.email.Email;

import android.net.ConnectivityManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Email");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(Email.BOOT_RECEIVER_WAKE_LOCK_TIMEOUT);
        // TODO: For now, let the wakeLock expire on its own, don't release it at the end of this method,
        // otherwise there's no point to it.  We're trying to give the MailService some time to start.
        
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
        else if (ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED.equals(intent.getAction())) {
            MailService.backgroundDataChanged(context);
        }
//        else if (Intent.ACTION_BATTERY_LOW.equals(intent.getAction())) {
//            MailService.batteryStatusChange(context, true);
//        }
//        else if (Intent.ACTION_BATTERY_OKAY.equals(intent.getAction())) {
//            MailService.batteryStatusChange(context, false);
//        }
        
    }
}
