package com.fsck.k9.power;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;

import timber.log.Timber;


public abstract class DeviceIdleManager {
    private static DeviceIdleManager instance;


    public static synchronized DeviceIdleManager getInstance(Context context) {
        if (instance == null) {
            DozeChecker dozeChecker = new DozeChecker(context);
            if (dozeChecker.isDeviceIdleModeSupported() && !dozeChecker.isAppWhitelisted()) {
                instance = RealDeviceIdleManager.newInstance(context);
            } else {
                instance = new NoOpDeviceIdleManager();
            }
        }
        return instance;
    }

    private DeviceIdleManager() {
    }

    public abstract void registerReceiver();
    public abstract void unregisterReceiver();


    static class NoOpDeviceIdleManager extends DeviceIdleManager {
        @Override
        public void registerReceiver() {
            // Do nothing
        }

        @Override
        public void unregisterReceiver() {
            // Do nothing
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    static class RealDeviceIdleManager extends DeviceIdleManager {
        private final Context context;
        private final DeviceIdleReceiver deviceIdleReceiver;
        private final IntentFilter intentFilter;
        private boolean registered;


        static RealDeviceIdleManager newInstance(Context context) {
            Context appContext = context.getApplicationContext();
            return new RealDeviceIdleManager(appContext);
        }

        private RealDeviceIdleManager(Context context) {
            this.context = context;
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            deviceIdleReceiver = new DeviceIdleReceiver(powerManager);
            intentFilter = new IntentFilter(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
        }

        @Override
        public void registerReceiver() {
            Timber.v("Registering DeviceIdleReceiver");
            registered = true;
            context.registerReceiver(deviceIdleReceiver, intentFilter);
        }

        @Override
        public void unregisterReceiver() {
            Timber.v("Unregistering DeviceIdleReceiver");
            if (registered) {
                context.unregisterReceiver(deviceIdleReceiver);
                registered = false;
            }
        }
    }
}
