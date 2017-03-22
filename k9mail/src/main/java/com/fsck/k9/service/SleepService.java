package com.fsck.k9.service;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.fsck.k9.mail.power.TracingPowerManager.TracingWakeLock;
import timber.log.Timber;

import static java.lang.Thread.currentThread;


public class SleepService extends CoreService {

    private static String ALARM_FIRED = "com.fsck.k9.service.SleepService.ALARM_FIRED";
    private static String LATCH_ID = "com.fsck.k9.service.SleepService.LATCH_ID_EXTRA";


    private static ConcurrentHashMap<Integer, SleepDatum> sleepData = new ConcurrentHashMap<Integer, SleepDatum>();

    private static AtomicInteger latchId = new AtomicInteger();

    public static void sleep(Context context, long sleepTime, TracingWakeLock wakeLock, long wakeLockTimeout) {
        Integer id = latchId.getAndIncrement();
        Timber.d("SleepService Preparing CountDownLatch with id = %d, thread %s", id, currentThread().getName());

        SleepDatum sleepDatum = new SleepDatum();
        CountDownLatch latch = new CountDownLatch(1);
        sleepDatum.latch = latch;
        sleepDatum.reacquireLatch = new CountDownLatch(1);
        sleepData.put(id, sleepDatum);

        Intent i = new Intent(context, SleepService.class);
        i.putExtra(LATCH_ID, id);
        i.setAction(ALARM_FIRED + "." + id);
        long startTime = SystemClock.elapsedRealtime();
        long nextTime = startTime + sleepTime;
        BootReceiver.scheduleIntent(context, nextTime, i);
        if (wakeLock != null) {
            sleepDatum.wakeLock = wakeLock;
            sleepDatum.timeout = wakeLockTimeout;
            wakeLock.release();
        }
        try {
            boolean countedDown = latch.await(sleepTime, TimeUnit.MILLISECONDS);
            if (!countedDown) {
                Timber.d("SleepService latch timed out for id = %d, thread %s", id, currentThread().getName());
            }
        } catch (InterruptedException ie) {
            Timber.e(ie, "SleepService Interrupted while awaiting latch");
        }
        SleepDatum releaseDatum = sleepData.remove(id);
        if (releaseDatum == null) {
            try {
                Timber.d("SleepService waiting for reacquireLatch for id = %d, thread %s",
                        id, currentThread().getName());

                if (!sleepDatum.reacquireLatch.await(5000, TimeUnit.MILLISECONDS)) {
                    Timber.w("SleepService reacquireLatch timed out for id = %d, thread %s",
                            id, currentThread().getName());
                } else {
                    Timber.d("SleepService reacquireLatch finished for id = %d, thread %s",
                            id, currentThread().getName());
                }
            } catch (InterruptedException ie) {
                Timber.e(ie, "SleepService Interrupted while awaiting reacquireLatch");
            }
        } else {
            reacquireWakeLock(releaseDatum);
        }

        long endTime = SystemClock.elapsedRealtime();
        long actualSleep = endTime - startTime;

        if (actualSleep < sleepTime) {
            Timber.w("SleepService sleep time too short: requested was %d, actual was %d", sleepTime, actualSleep);
        } else {
            Timber.d("SleepService requested sleep time was %d, actual was %d", sleepTime, actualSleep);
        }
    }

    private static void endSleep(Integer id) {
        if (id != -1) {
            SleepDatum sleepDatum = sleepData.remove(id);
            if (sleepDatum != null) {
                CountDownLatch latch = sleepDatum.latch;
                if (latch == null) {
                    Timber.e("SleepService No CountDownLatch available with id = %s", id);
                } else {
                    Timber.d("SleepService Counting down CountDownLatch with id = %d", id);
                    latch.countDown();
                }
                reacquireWakeLock(sleepDatum);
                sleepDatum.reacquireLatch.countDown();
            } else {
                Timber.d("SleepService Sleep for id %d already finished", id);
            }
        }
    }

    private static void reacquireWakeLock(SleepDatum sleepDatum) {
        TracingWakeLock wakeLock = sleepDatum.wakeLock;
        if (wakeLock != null) {
            synchronized (wakeLock) {
                long timeout = sleepDatum.timeout;
                Timber.d("SleepService Acquiring wakeLock for %d ms", timeout);
                wakeLock.acquire(timeout);
            }
        }
    }

    @Override
    public int startService(Intent intent, int startId) {
        try {
          if (intent.getAction().startsWith(ALARM_FIRED)) {
              Integer id = intent.getIntExtra(LATCH_ID, -1);
              endSleep(id);
          }
          return START_NOT_STICKY;
        }
        finally {
          stopSelf(startId);
        }
    }

    private static class SleepDatum {
        CountDownLatch latch;
        TracingWakeLock wakeLock;
        long timeout;
        CountDownLatch reacquireLatch;
    }

}
