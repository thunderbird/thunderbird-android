package com.fsck.k9.service;

import android.content.Context;
import android.content.Intent;
import timber.log.Timber;
import com.fsck.k9.K9;
import com.fsck.k9.mail.power.TracingPowerManager.TracingWakeLock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.currentThread;


public class SleepService extends CoreService {

    private static String ALARM_FIRED = "com.fsck.k9.service.SleepService.ALARM_FIRED";
    private static String LATCH_ID = "com.fsck.k9.service.SleepService.LATCH_ID_EXTRA";


    private static ConcurrentHashMap<Integer, SleepDatum> sleepData = new ConcurrentHashMap<Integer, SleepDatum>();

    private static AtomicInteger latchId = new AtomicInteger();

    public static void sleep(Context context, long sleepTime, TracingWakeLock wakeLock, long wakeLockTimeout) {
        Integer id = latchId.getAndIncrement();
        if (K9.DEBUG)
            Timber.d("SleepService Preparing CountDownLatch with id = " + id + ", thread " +
                    currentThread().getName());
        SleepDatum sleepDatum = new SleepDatum();
        CountDownLatch latch = new CountDownLatch(1);
        sleepDatum.latch = latch;
        sleepDatum.reacquireLatch = new CountDownLatch(1);
        sleepData.put(id, sleepDatum);

        Intent i = new Intent(context, SleepService.class);
        i.putExtra(LATCH_ID, id);
        i.setAction(ALARM_FIRED + "." + id);
        long startTime = System.currentTimeMillis();
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
                if (K9.DEBUG)
                    Timber.d("SleepService latch timed out for id = " + id + ", thread " +
                            currentThread().getName());
            }
        } catch (InterruptedException ie) {
            Timber.e("SleepService Interrupted while awaiting latch", ie);
        }
        SleepDatum releaseDatum = sleepData.remove(id);
        if (releaseDatum == null) {
            try {
                if (K9.DEBUG)
                    Timber.d("SleepService waiting for reacquireLatch for id = " + id + ", thread " +
                            currentThread().getName());
                if (!sleepDatum.reacquireLatch.await(5000, TimeUnit.MILLISECONDS)) {
                    Timber.w("SleepService reacquireLatch timed out for id = " + id + ", thread " +
                            currentThread().getName());
                } else if (K9.DEBUG)
                    Timber.d("SleepService reacquireLatch finished for id = " + id + ", thread " +
                            currentThread().getName());
            } catch (InterruptedException ie) {
                Timber.e("SleepService Interrupted while awaiting reacquireLatch", ie);
            }
        } else {
            reacquireWakeLock(releaseDatum);
        }

        long endTime = System.currentTimeMillis();
        long actualSleep = endTime - startTime;

        if (actualSleep < sleepTime) {
            Timber.w("SleepService sleep time too short: requested was " + sleepTime + ", actual was " + actualSleep);
        } else {
            if (K9.DEBUG)
                Timber.d("SleepService requested sleep time was " + sleepTime + ", actual was " + actualSleep);
        }
    }

    private static void endSleep(Integer id) {
        if (id != -1) {
            SleepDatum sleepDatum = sleepData.remove(id);
            if (sleepDatum != null) {
                CountDownLatch latch = sleepDatum.latch;
                if (latch == null) {
                    Timber.e("SleepService No CountDownLatch available with id = " + id);
                } else {
                    if (K9.DEBUG)
                        Timber.d("SleepService Counting down CountDownLatch with id = " + id);
                    latch.countDown();
                }
                reacquireWakeLock(sleepDatum);
                sleepDatum.reacquireLatch.countDown();
            } else {
                if (K9.DEBUG)
                    Timber.d("SleepService Sleep for id " + id + " already finished");
            }
        }
    }

    private static void reacquireWakeLock(SleepDatum sleepDatum) {
        TracingWakeLock wakeLock = sleepDatum.wakeLock;
        if (wakeLock != null) {
            synchronized (wakeLock) {
                long timeout = sleepDatum.timeout;
                if (K9.DEBUG)
                    Timber.d("SleepService Acquiring wakeLock for " + timeout + "ms");
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
