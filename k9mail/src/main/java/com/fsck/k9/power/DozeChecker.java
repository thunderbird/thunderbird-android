package com.fsck.k9.power;


import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;


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

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean isAppWhitelisted() {
        return powerManager.isIgnoringBatteryOptimizations(packageName);
    }
}
