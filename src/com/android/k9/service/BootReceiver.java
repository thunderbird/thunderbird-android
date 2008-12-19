
package com.fsck.k9.service;

import com.fsck.k9.MessagingController;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            MailService.actionReschedule(context);
        }
        else if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(intent.getAction())) {
            MailService.actionCancel(context);
        }
        else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(intent.getAction())) {
            MailService.actionReschedule(context);
        }
    }
}
