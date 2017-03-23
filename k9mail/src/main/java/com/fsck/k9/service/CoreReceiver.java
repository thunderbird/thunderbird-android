
package com.fsck.k9.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import timber.log.Timber;

import com.fsck.k9.K9;
import com.fsck.k9.mail.power.TracingPowerManager;
import com.fsck.k9.mail.power.TracingPowerManager.TracingWakeLock;

public class CoreReceiver extends BroadcastReceiver {

    public static final String WAKE_LOCK_RELEASE = "com.fsck.k9.service.CoreReceiver.wakeLockRelease";

    public static final String WAKE_LOCK_ID = "com.fsck.k9.service.CoreReceiver.wakeLockId";

    private static ConcurrentHashMap<Integer, TracingWakeLock> wakeLocks = new ConcurrentHashMap<Integer, TracingWakeLock>();
    private static AtomicInteger wakeLockSeq = new AtomicInteger(0);

    private static Integer getWakeLock(Context context) {
        TracingPowerManager pm = TracingPowerManager.getPowerManager(context);
        TracingWakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CoreReceiver getWakeLock");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(K9.BOOT_RECEIVER_WAKE_LOCK_TIMEOUT);
        Integer tmpWakeLockId = wakeLockSeq.getAndIncrement();
        wakeLocks.put(tmpWakeLockId, wakeLock);
        Timber.v("CoreReceiver Created wakeLock %d", tmpWakeLockId);
        return tmpWakeLockId;
    }

    private static void releaseWakeLock(Integer wakeLockId) {
        if (wakeLockId != null) {
            TracingWakeLock wl = wakeLocks.remove(wakeLockId);
            if (wl != null) {
                Timber.v("CoreReceiver Releasing wakeLock %d", wakeLockId);
                wl.release();
            } else {
                Timber.w("BootReceiver WakeLock %d doesn't exist", wakeLockId);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Integer tmpWakeLockId = CoreReceiver.getWakeLock(context);
        try {
            Timber.i("CoreReceiver.onReceive %s", intent);

            if (CoreReceiver.WAKE_LOCK_RELEASE.equals(intent.getAction())) {
                Integer wakeLockId = intent.getIntExtra(WAKE_LOCK_ID, -1);
                if (wakeLockId != -1) {
                    Timber.v("CoreReceiver Release wakeLock %d", wakeLockId);
                    CoreReceiver.releaseWakeLock(wakeLockId);
                }
            } else {
                tmpWakeLockId = receive(context, intent, tmpWakeLockId);
            }
        } finally {
            CoreReceiver.releaseWakeLock(tmpWakeLockId);
        }
    }

    public Integer receive(Context context, Intent intent, Integer wakeLockId) {
        return wakeLockId;
    }

    public static void releaseWakeLock(Context context, int wakeLockId) {
        Timber.v("CoreReceiver Got request to release wakeLock %d", wakeLockId);

        Intent i = new Intent();
        i.setClass(context, CoreReceiver.class);
        i.setAction(WAKE_LOCK_RELEASE);
        i.putExtra(WAKE_LOCK_ID, wakeLockId);
        context.sendBroadcast(i);
    }
}
