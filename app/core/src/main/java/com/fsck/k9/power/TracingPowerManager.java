package com.fsck.k9.power;


import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;

import com.fsck.k9.mail.K9MailLib;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;


public class TracingPowerManager implements com.fsck.k9.mail.power.PowerManager {
    private final static boolean TRACE = false;
    public static AtomicInteger wakeLockId = new AtomicInteger(0);
    PowerManager pm = null;
    private static TracingPowerManager tracingPowerManager;
    private Timer timer = null;

    public static synchronized TracingPowerManager getPowerManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (tracingPowerManager == null) {
            if (K9MailLib.isDebug()) {
                Timber.v("Creating TracingPowerManager");
            }
            tracingPowerManager = new TracingPowerManager(appContext);
        }
        return tracingPowerManager;
    }


    private TracingPowerManager(Context context) {
        pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (TRACE) {
            timer = new Timer();
        }
    }

    @NotNull
    @Override
    public com.fsck.k9.mail.power.WakeLock newWakeLock(@NotNull String tag) {
        return new TracingWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
    }

    public TracingWakeLock newWakeLock(int flags, String tag) {
        return new TracingWakeLock(flags, tag);
    }


    public class TracingWakeLock implements com.fsck.k9.mail.power.WakeLock {
        final WakeLock wakeLock;
        final int id;
        final String tag;
        volatile TimerTask timerTask;
        volatile Long startTime = null;
        volatile Long timeout = null;

        public TracingWakeLock(int flags, String ntag) {
            tag = ntag;
            wakeLock = pm.newWakeLock(flags, tag);
            id = wakeLockId.getAndIncrement();
            if (K9MailLib.isDebug()) {
                Timber.v("TracingWakeLock for tag %s / id %d: Create", tag, id);
            }
        }

        @Override
        public void acquire(long timeout) {
            synchronized (wakeLock) {
                wakeLock.acquire(timeout);
            }
            if (K9MailLib.isDebug()) {
                Timber.v("TracingWakeLock for tag %s / id %d for %d ms: acquired", tag, id, timeout);
            }
            raiseNotification();
            if (startTime == null) {
                startTime = SystemClock.elapsedRealtime();
            }
            this.timeout = timeout;
        }

        @Override
        public void acquire() {
            synchronized (wakeLock) {
                wakeLock.acquire();
            }
            raiseNotification();
            if (K9MailLib.isDebug()) {
                Timber.w("TracingWakeLock for tag %s / id %d: acquired with no timeout.  K-9 Mail should not do this",
                        tag, id);
            }
            if (startTime == null) {
                startTime = SystemClock.elapsedRealtime();
            }
            timeout = null;
        }

        @Override
        public void setReferenceCounted(boolean counted) {
            synchronized (wakeLock) {
                wakeLock.setReferenceCounted(counted);
            }
        }

        @Override
        public void release() {
            if (startTime != null) {
                Long endTime = SystemClock.elapsedRealtime();
                if (K9MailLib.isDebug()) {
                    Timber.v("TracingWakeLock for tag %s / id %d: releasing after %d ms, timeout = %d ms",
                            tag, id, endTime - startTime, timeout);
                }
            } else {
                if (K9MailLib.isDebug()) {
                    Timber.v("TracingWakeLock for tag %s / id %d, timeout = %d ms: releasing", tag, id, timeout);
                }
            }
            cancelNotification();
            synchronized (wakeLock) {
                wakeLock.release();
            }
            startTime = null;
        }

        private void cancelNotification() {
            if (timer != null) {
                synchronized (timer) {
                    if (timerTask != null) {
                        timerTask.cancel();
                    }
                }
            }
        }

        private void raiseNotification() {
            if (timer != null) {
                synchronized (timer) {
                    if (timerTask != null) {
                        timerTask.cancel();
                        timerTask = null;
                    }
                    timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            if (startTime != null) {
                                Long endTime = SystemClock.elapsedRealtime();
                                Timber.i("TracingWakeLock for tag %s / id %d: has been active for %d ms, timeout = %d ms",
                                        tag, id, endTime - startTime, timeout);

                            } else {
                                Timber.i("TracingWakeLock for tag %s / id %d: still active, timeout = %d ms",
                                        tag, id, timeout);
                            }
                        }

                    };
                    timer.schedule(timerTask, 1000, 1000);
                }
            }
        }
    }
}
