package com.fsck.k9.power;


import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.service.MailService;


@TargetApi(Build.VERSION_CODES.M)
class DeviceIdleReceiver extends BroadcastReceiver {
    private final PowerManager powerManager;


    DeviceIdleReceiver(PowerManager powerManager) {
        this.powerManager = powerManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean deviceInIdleMode = powerManager.isDeviceIdleMode();
        Log.v(K9.LOG_TAG, "Device idle mode changed. Idle: " + deviceInIdleMode);

        if (!deviceInIdleMode) {
            MailService.actionReset(context, null);
        }
    }
}
