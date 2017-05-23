package com.fsck.k9.power;


import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;


public class DozeChecker {
    private final PowerManager powerManager;
    private final String packageName;


    public DozeChecker(Context context) {
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        packageName = context.getPackageName();
    }

    public boolean isDeviceIdleModeSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean isAppWhitelisted() {
        return powerManager.isIgnoringBatteryOptimizations(packageName);
    }
}
