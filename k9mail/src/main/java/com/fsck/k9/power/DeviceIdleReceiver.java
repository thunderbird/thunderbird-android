package com.fsck.k9.power;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;

import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.service.BootReceiver;
import com.fsck.k9.service.MailService;
import com.fsck.k9.service.PollService;
import timber.log.Timber;


@RequiresApi(api = Build.VERSION_CODES.M)
class DeviceIdleReceiver extends BroadcastReceiver {
    private final PowerManager powerManager;


    DeviceIdleReceiver(PowerManager powerManager) {
        this.powerManager = powerManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean deviceInIdleMode = powerManager.isDeviceIdleMode();

        if (deviceInIdleMode) {
            Timber.v("Device entering doze mode");
            BootReceiver.purgeSchedule(context);
            PollService.stopService(context);
            MailService.actionStopPushers(context, null);
        } else {
            boolean deviceInteractive = powerManager.isInteractive();
            if (deviceInteractive) {
                Timber.v("Device exiting doze mode");
                MailService.actionReset(context, null);
            } else {
                Timber.v("Device entering doze maintenance window");
                MessagingController controller = MessagingController.getInstance(context);
                controller.setCheckMailListener(null);
                controller.checkMail(context, null, true, true, null);
            }
        }
    }
}
