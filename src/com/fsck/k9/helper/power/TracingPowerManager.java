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
    private final static boolean TRACE = true;
    public static AtomicInteger wakeLockId = new AtomicInteger(0);
    PowerManager pm = null;
    private static TracingPowerManager tracingPowerManager;
    private Timer timer;
    
    public static synchronized TracingPowerManager getPowerManager(Context context)
    {
        if (tracingPowerManager == null)
        {
            if (K9.DEBUG)
            {
                Log.i(K9.LOG_TAG, "Creating TracingPowerManager");
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
        WakeLock wakeLock = null;
        int id;
        String tag;
        TimerTask timerTask;
        public TracingWakeLock(int flags, String ntag)
        {
            tag = ntag;
            wakeLock = pm.newWakeLock(flags, tag);
            id = wakeLockId.getAndIncrement();
            if (K9.DEBUG)
            {
                Log.i(K9.LOG_TAG, "Creating TracingWakeLock for tag " + tag + " with id " + id);
            }
        }
        public void acquire(long timeout)
        {
            if (K9.DEBUG)
            {
                Log.i(K9.LOG_TAG, "Acquiring TracingWakeLock for tag " + tag + " and id " + id 
                        + " for " + timeout + " ms");
            }
            raiseNotification();
            wakeLock.acquire(timeout);
            
        }
        public void acquire()
        {
            if (K9.DEBUG)
            {
                Log.i(K9.LOG_TAG, "Acquiring TracingWakeLock for tag " + tag + " and id " + id 
                        + " with no timeout");
            }
            raiseNotification();
            wakeLock.acquire();
        }
        public void setReferenceCounted(boolean counted)
        {
            wakeLock.setReferenceCounted(counted);
        }
        public void release()
        {
            if (K9.DEBUG)
            {
                Log.i(K9.LOG_TAG, "Releasing TracingWakeLock for tag " + tag + " and id " + id );
            }
            wakeLock.release();
            cancelNotification();
            
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
                           Log.i(K9.LOG_TAG, "TracingWakeLock for tag " + tag + " and id " + id + " still active");
                        }
                        
                    };
                    timer.schedule(timerTask, 1000, 1000);
                }
            }
        }
        
    }
}
