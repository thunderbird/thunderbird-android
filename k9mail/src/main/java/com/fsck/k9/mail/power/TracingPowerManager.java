package com.fsck.k9.mail.power;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import com.fsck.k9.mail.K9MailLib;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;


public class TracingPowerManager {
    private final static boolean TRACE = false;
    public static AtomicInteger wakeLockId = new AtomicInteger(0);
    PowerManager pm = null;
    private static TracingPowerManager tracingPowerManager;
    private Timer timer = null;

    public static synchronized TracingPowerManager getPowerManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (tracingPowerManager == null) {
            if (K9MailLib.isDebug()) {
                Log.v(LOG_TAG, "Creating TracingPowerManager");
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

    public TracingWakeLock newWakeLock(int flags, String tag) {
        return new TracingWakeLock(flags, tag);
    }
    public class TracingWakeLock {
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
                Log.v(LOG_TAG, "TracingWakeLock for tag " + tag + " / id " + id + ": Create");
            }
        }
        public void acquire(long timeout) {
            synchronized (wakeLock) {
                wakeLock.acquire(timeout);
            }
            if (K9MailLib.isDebug()) {
                Log.v(LOG_TAG, "TracingWakeLock for tag " + tag + " / id " + id + " for " + timeout + " ms: acquired");
            }
            raiseNotification();
            if (startTime == null) {
                startTime = System.currentTimeMillis();
            }
            this.timeout = timeout;
        }
        public void acquire() {
            synchronized (wakeLock) {
                wakeLock.acquire();
            }
            raiseNotification();
            if (K9MailLib.isDebug()) {
                Log.w(LOG_TAG, "TracingWakeLock for tag " + tag + " / id " + id + ": acquired with no timeout.  K-9 Mail should not do this");
            }
            if (startTime == null) {
                startTime = System.currentTimeMillis();
            }
            timeout = null;
        }
        public void setReferenceCounted(boolean counted) {
            synchronized (wakeLock) {
                wakeLock.setReferenceCounted(counted);
            }
        }
        public void release() {
            if (startTime != null) {
                Long endTime = System.currentTimeMillis();
                if (K9MailLib.isDebug()) {
                    Log.v(LOG_TAG, "TracingWakeLock for tag " + tag + " / id " + id + ": releasing after " + (endTime - startTime) + " ms, timeout = " + timeout + " ms");
                }
            } else {
                if (K9MailLib.isDebug()) {
                    Log.v(LOG_TAG, "TracingWakeLock for tag " + tag + " / id " + id + ", timeout = " + timeout + " ms: releasing");
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
                                Long endTime = System.currentTimeMillis();
                                Log.i(LOG_TAG, "TracingWakeLock for tag " + tag + " / id " + id + ": has been active for "
                                      + (endTime - startTime) + " ms, timeout = " + timeout + " ms");

                            } else {
                                Log.i(LOG_TAG, "TracingWakeLock for tag " + tag + " / id " + id + ": still active, timeout = " + timeout + " ms");
                            }
                        }

                    };
                    timer.schedule(timerTask, 1000, 1000);
                }
            }
        }

    }
}
