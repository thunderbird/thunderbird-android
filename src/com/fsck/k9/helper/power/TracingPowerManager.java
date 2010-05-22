package com.fsck.k9.helper.power;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import com.fsck.k9.K9;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class TracingPowerManager
{
    private final static boolean TRACE = false;
    public static AtomicInteger wakeLockId = new AtomicInteger(0);
    PowerManager pm = null;
    private static TracingPowerManager tracingPowerManager;
    private Timer timer = null;
    
    public static synchronized TracingPowerManager getPowerManager(Context context)
    {
        if (tracingPowerManager == null)
        {
            if (K9.DEBUG)
            {
                Log.v(K9.LOG_TAG, "Creating TracingPowerManager");
            }
            tracingPowerManager = new TracingPowerManager(context);
        }
        return tracingPowerManager;
    }
    
    
    private TracingPowerManager(Context context)
    {
         pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
         if (TRACE)
         {
             timer = new Timer();
         }
    }
    
    public TracingWakeLock newWakeLock(int flags, String tag)
    {
        return new TracingWakeLock(flags, tag);
    }
    public class TracingWakeLock
    {
        final WakeLock wakeLock;
        final int id;
        final String tag;
        volatile TimerTask timerTask;
        volatile Long startTime = null;
        volatile Long timeout = null;
        public TracingWakeLock(int flags, String ntag)
        {
            tag = ntag;
            wakeLock = pm.newWakeLock(flags, tag);
            id = wakeLockId.getAndIncrement();
            if (K9.DEBUG)
            {
                Log.v(K9.LOG_TAG, "Creating TracingWakeLock for tag " + tag + " with id " + id);
            }
        }
        public void acquire(long timeout)
        {
            synchronized(wakeLock)
            {
                wakeLock.acquire(timeout);
            }
            if (K9.DEBUG)
            {
                Log.v(K9.LOG_TAG, "Acquired TracingWakeLock for tag " + tag + " and id " + id 
                        + " for " + timeout + " ms");
            }
            raiseNotification();
            if (startTime == null)
            {
                startTime = System.currentTimeMillis();
            }
            this.timeout = timeout;
        }
        public void acquire()
        {
            synchronized(wakeLock)
            {
                wakeLock.acquire();
            }
            raiseNotification();
            if (K9.DEBUG)
            {
                Log.w(K9.LOG_TAG, "Acquired TracingWakeLock for tag " + tag + " and id " + id 
                        + " with no timeout.  K-9 Mail should not do this");
            }
            if (startTime == null)
            {
                startTime = System.currentTimeMillis();
            }
            timeout = null;
        }
        public void setReferenceCounted(boolean counted)
        {
            synchronized(wakeLock)
            {
                wakeLock.setReferenceCounted(counted);
            }
        }
        public void release()
        {
            if (startTime != null)
            {
                Long endTime = System.currentTimeMillis();
                if (K9.DEBUG)
                {
                    Log.v(K9.LOG_TAG, "Releasing TracingWakeLock for tag " + tag + " and id " + id + " after " 
                            + (endTime - startTime) + " ms, timeout = " + timeout + " ms");
                }
            }
            else
            {
                if (K9.DEBUG)
                {
                    Log.v(K9.LOG_TAG, "Releasing TracingWakeLock for tag " + tag + " and id " + id + ", timeout = " + timeout + " ms");
                }
            }
            cancelNotification();
            synchronized(wakeLock)
            {
                wakeLock.release();
            }
            startTime = null;
        }
        private void cancelNotification()
        {
            if (timer != null)
            {
                synchronized(timer)
                {
                    if (timerTask != null)
                    {
                        timerTask.cancel();
                    }
                }
            }
        }
        private void raiseNotification()
        {
            if (timer != null)
            {
                synchronized(timer)
                {
                    if (timerTask != null)
                    {
                        timerTask.cancel();
                        timerTask = null;
                    }
                    timerTask = new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            if (startTime != null)
                            {
                                Long endTime = System.currentTimeMillis();
                                Log.i(K9.LOG_TAG, "TracingWakeLock for tag " + tag + " and id " + id + " has been active for " 
                                        + (endTime - startTime) + " ms, timeout = " + timeout + " ms");
                                
                            }
                            else
                            {
                                Log.i(K9.LOG_TAG, "TracingWakeLock for tag " + tag + " and id " + id + " still active, timeout = " + timeout + " ms");
                            }
                        }
                        
                    };
                    timer.schedule(timerTask, 1000, 1000);
                }
            }
        }
        
    }
}
