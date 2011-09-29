package com.fsck.k9.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import com.fsck.k9.K9;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.helper.power.TracingPowerManager;
import com.fsck.k9.helper.power.TracingPowerManager.TracingWakeLock;

/**
 * Note: All documentation in this file market <CK:RE> is documentation written by Christian Knecht by reverse engineering.
 *       This documentation is without warranty and may not be accurate nor reflect the author's original intent.
 *      
 * <CK:RE>
 * CoreService is the base class for all K9 Services.
 * 
 * An Android service is a way to model a part of an application that needs to accomplish certain tasks without the
 * UI part of the application being necessarily active (of course an application could also be a pure service, without
 * any UI; this is not the case of K9). By declaring a service and starting it, the OS knows that the application has
 * work to do and should avoid killing the process.
 * 
 * A service's main purpose is to do some task (usually in the background) which requires one of more threads. The
 * thread that starts the service is the same as the UI thread of the process. It should thus not be used to run
 * the tasks.
 * 
 * CoreService is providing the execution plumbing for background tasks including the required thread and task queuing
 * for all K9 services to use.
 * 
 * A service is supposed to run only as long as it has some work to do whether that work is active processing or some
 * just some monitoring, like listening on a network port for incoming connections or listing on an open network
 * connection for incoming data (push mechanism).
 * 
 * To make sure the service is running only when required, is must be shutdown after tasks are done. As the
 * execution of tasks is abstracted away in this class, it also proper shutdown handling if approriate. If
 * the Service requires this is should call enableAutoShutdown(true) in it's onCreate() method. 
 *
 * While a service is running it's tasks, it is usually not a good idea to let the device go to sleep more.
 * WakeLocks are used to avoid this. CoreService provides a central registry (singleton) that can be used
 * application-wide to store WakeLocks.
 * 
 * In short, CoreService provides the following features to K9 Services:
 *  - task execution and queuing
 *  - Service life cycle management (insures the service is stopped when not needed anymore); disabled by default
 *  - WakeLock registry and management
 *  
 * </CK:RE>      
 */
public abstract class CoreService extends Service {

    public static String WAKE_LOCK_ID = "com.fsck.k9.service.CoreService.wakeLockId"; // CK:Intent attribute ID
    private static ConcurrentHashMap<Integer, TracingWakeLock> wakeLocks = new ConcurrentHashMap<Integer, TracingWakeLock>(); // CK:WakeLocks registry
    private static AtomicInteger wakeLockSeq = new AtomicInteger(0); // CK:WakeLock registry
    private ExecutorService threadPool = null; // CK:Threadpool with a single thread; used to execute and queue background actions inside the service
    private final String className = getClass().getName();
    private volatile boolean mShutdown = false; // CK:A:Seems to be used only when the service is "officially" shutdown to make sure that an exception raise because of the shutdown gets ignored.

    /**
     * Controls the auto-shutdown mechanism of the service. The default service life-cycle model is that the service should run
     * only as long as a task is running. If a service should behave differently, disable auto-shutdown.
     */
    private boolean mAutoShutdown = true;
    
    /**
     * This variable is part of the auto-shutdown feature and determines whether the service has to be shutdown at the
     * end of the onStart() method or not.
     */
    protected boolean mImmediateShutdown = true; // 
    
    @Override
    public void onCreate() {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "CoreService: " + className + ".onCreate()");
        threadPool = Executors.newFixedThreadPool(1);  // Must be single threaded
        super.onCreate();

    }

    /**
     * Adds an existing WakeLock identified by it's WakeLock-ID to the specified Intent.
     * @param i
     * @param wakeLockId
     */
    protected static void addWakeLockId(Context context, Intent i, Integer wakeLockId, boolean createIfNotExists) {
        if (wakeLockId != null) {
            i.putExtra(BootReceiver.WAKE_LOCK_ID, wakeLockId);
            return;
        }
        if (createIfNotExists)
          addWakeLock(context,i);
    }

    /**
     * Adds a new WakeLock to the intent.
     * This will add the WakeLock to the central WakeLock registry managed by this class.
     * @param context Required to be able to create a new wake-lock.
     * @param i Intent to which to add the WakeLock (CK:Q:still unclear why we need to link Intents and WakeLocks)
     */
    protected static void addWakeLock(Context context, Intent i) {
        TracingWakeLock wakeLock = acquireWakeLock(context,"CoreService addWakeLock",K9.MAIL_SERVICE_WAKE_LOCK_TIMEOUT); // CK:Q: What this timeout? 30secs seems a bizarre choice. It it's a safeguard it should be longer, if it's to cover the real time required by some operation, it seems too short (though I say this before knowing really what the service is supposed to do)
        Integer tmpWakeLockId = registerWakeLock(wakeLock);
        i.putExtra(WAKE_LOCK_ID, tmpWakeLockId);
    }

    /**
     * Register WakeLock and returns its registry-entry-ID
     * @param wakeLock
     * @return
     * AUTHOR chrisk
     */
    protected static Integer registerWakeLock(TracingWakeLock wakeLock) {
        Integer tmpWakeLockId = wakeLockSeq.getAndIncrement();
        wakeLocks.put(tmpWakeLockId, wakeLock);
        return tmpWakeLockId;
    }

    /**
     * Acquires a WakeLock in a K9 standard way
     * @param context
     * @return
     * AUTHOR chrisk
     */
    protected static TracingWakeLock acquireWakeLock(Context context, String tag, long timeout) {
        TracingPowerManager pm = TracingPowerManager.getPowerManager(context);
        TracingWakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(timeout);
        return wakeLock;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        // deprecated method but still used for backwards compatibility with Android version <2.0
      
        // CK:DocAdded: Manage wake-locks, especially, release any wake-locks held so far and define a new "local" wake lock.
        //           Also, because we create a new wakelock, we re-initialize the wakelock timeout and give
        //           the service-start code a protection of up to MAIL_SERVICE_WAKE_LOCK_TIMEOUT (currently 30s).
        TracingWakeLock wakeLock = acquireWakeLock(this,"CoreService onStart",K9.MAIL_SERVICE_WAKE_LOCK_TIMEOUT);

        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "CoreService: " + className + ".onStart(" + intent + ", " + startId);

        int wakeLockId = intent.getIntExtra(BootReceiver.WAKE_LOCK_ID, -1);
        if (wakeLockId != -1) {
            BootReceiver.releaseWakeLock(this, wakeLockId);
        }
        Integer coreWakeLockId = intent.getIntExtra(WAKE_LOCK_ID, -1);
        if (coreWakeLockId != null && coreWakeLockId != -1) {
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "Got core wake lock id " + coreWakeLockId);
            TracingWakeLock coreWakeLock = wakeLocks.remove(coreWakeLockId);
            if (coreWakeLock != null) {
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "Found core wake lock with id " + coreWakeLockId + ", releasing");
                coreWakeLock.release();
            }
        }

        // Run the actual start-code of the service
        mImmediateShutdown = true;
        try {
            super.onStart(intent, startId);
            startService(intent, startId);
        } finally {
            try{wakeLock.release();} catch (Exception e) {/* ignore */}
            try{if (mAutoShutdown && mImmediateShutdown && startId != -1) stopSelf(startId);} catch (Exception e) {/* ignore */} 
        }
    }

    /**
     * 
     * @param context
     * @param runner
     * @param wakeLockTime
     * @param startId
     * @return returns whether service-shutdown will actually happen after the task has been executed (or has already been done).
     */
    public boolean execute(Context context, final Runnable runner, int wakeLockTime, final Integer startId) {

        boolean serviceShutdownScheduled = false;
        final TracingWakeLock wakeLock = acquireWakeLock(context,"CoreService execute",wakeLockTime);
        final boolean autoShutdown = mAutoShutdown;

        Runnable myRunner = new Runnable() {
            public void run() {
                try {
                    boolean oldIsSyncDisabled = MailService.isSyncDisabled();
                    if (K9.DEBUG)
                        Log.d(K9.LOG_TAG, "CoreService (" + className + ") running Runnable " + runner.hashCode() + " with startId " + startId);
                    runner.run();
                    if (MailService.isSyncDisabled() != oldIsSyncDisabled) {
                        MessagingController.getInstance(getApplication()).systemStatusChanged();
                    }
                } finally {
                    try { // Making absolutely sure the service stopping command will be executed
                        if (K9.DEBUG)
                            Log.d(K9.LOG_TAG, "CoreService (" + className + ") completed Runnable " + runner.hashCode() + " with startId " + startId);
                        wakeLock.release();
                    } finally {
                        if (autoShutdown && startId != null) {
                            stopSelf(startId); // <-- this is what is meant with "serviceShutdownScheduled"; execution of this line assures proper shutdown of the service once finished
                        }
                    }
                }
            }

        };
        if (threadPool == null) {
            Log.e(K9.LOG_TAG, "CoreService.execute (" + className + ") called with no threadPool available; running Runnable " + runner.hashCode() + " in calling thread", new Throwable());
            synchronized (this) {
                myRunner.run();
                serviceShutdownScheduled = startId != null; // In this case it's not actually scheduled, it's already done, but that should never happen anyway
            }
        } else {
            if (K9.DEBUG)
                Log.d(K9.LOG_TAG, "CoreService (" + className + ") queueing Runnable " + runner.hashCode() + " with startId " + startId);
            try {
                threadPool.execute(myRunner);
                serviceShutdownScheduled = startId != null;
            } catch (RejectedExecutionException e) {
                if (!mShutdown) {
                    throw e;
                }
                Log.i(K9.LOG_TAG, "CoreService: " + className + " is shutting down, ignoring rejected execution exception: " + e.getMessage());
            }
        }
        mImmediateShutdown = !serviceShutdownScheduled; 
        return serviceShutdownScheduled;
    }

    /**
     * CK:Added
     * To implement by sub-class instead of overriding onStart.
     * This allows CoreService to do start and end operations around the sub-class's start code.
     * Especially, CoreService will protect the start-code with a wake-lock to guarantee the service to have the required resources to do it's work.
     * CK:Q: Is this really useful (the wakelock part)? The real work is happening in the worker-thread anyway. Maybe it is because this makes sure that whatever needs to be started by the service, it can be without being interrupted by the phone going to sleep.
     * @param intent
     * @param startId
     */
    public abstract void startService(Intent intent, int startId);

    @Override
    public IBinder onBind(@SuppressWarnings("unused") Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onLowMemory() {
        Log.w(K9.LOG_TAG, "CoreService: " + className + ".onLowMemory() - Running low on memory");
    }

    @Override
    public void onDestroy() {
        if (K9.DEBUG)
            Log.i(K9.LOG_TAG, "CoreService: " + className + ".onDestroy()");
        mShutdown = true;
        threadPool.shutdown();
        super.onDestroy();
        //     MessagingController.getInstance(getApplication()).removeListener(mListener);
    }

    /**
     * @return True if auto-shutdown is enabled
     */
    protected boolean isAutoShutdown() {
        return mAutoShutdown;
    }

    /**
     * Enable of disable auto-shutdown (enabled by default).
     * See {@#mAutoShutdown} for more information.
     * @param autoShutdown
     */
    protected void setAutoShutdown(boolean autoShutdown) {
        mAutoShutdown = autoShutdown;
    }
}
