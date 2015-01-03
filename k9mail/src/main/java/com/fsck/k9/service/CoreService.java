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
import com.fsck.k9.mail.power.TracingPowerManager;
import com.fsck.k9.mail.power.TracingPowerManager.TracingWakeLock;

/**
 * {@code CoreService} is the base class for all K-9 Services.
 *
 * <p>
 * An Android service is a way to model a part of an application that needs to accomplish certain
 * tasks without the UI part of the application being necessarily active (of course an application
 * could also be a pure service, without any UI; this is not the case of K-9). By declaring a
 * service and starting it, the OS knows that the application has work to do and should avoid
 * killing the process.
 * </p><p>
 * A service's main purpose is to do some task (usually in the background) which requires one or
 * more threads. The thread that starts the service is the same as the UI thread of the process. It
 * should thus not be used to run the tasks.
 * </p><p>
 * CoreService is providing the execution plumbing for background tasks including the required
 * thread and task queuing for all K9 services to use.
 * </p><p>
 * A service is supposed to run only as long as it has some work to do whether that work is active
 * processing or some just some monitoring, like listening on a network port for incoming connections
 * or listing on an open network connection for incoming data (push mechanism).
 * </p><p>
 * To make sure the service is running only when required, is must be shutdown after tasks are
 * done. As the execution of tasks is abstracted away in this class, it also handles proper
 * shutdown. If a Service doesn't want this, it needs to call {@code enableAutoShutdown(true)} in
 * its {@link Service#onCreate()} method.
 * </p><p>
 * While a service is running its tasks, it is usually not a good idea to let the device go to
 * sleep mode. Wake locks are used to avoid this. CoreService provides a central registry
 * (singleton) that can be used application-wide to store wake locks.
 * </p><p>
 * In short, CoreService provides the following features to K-9 Services:
 * <ul>
 *   <li>task execution and queuing</li>
 *   <li>Service life cycle management (ensures the service is stopped when not needed anymore);
 *       enabled by default</li>
 *   <li>wake lock registry and management</li>
 * </ul>
 */
public abstract class CoreService extends Service {

    public static final String WAKE_LOCK_ID = "com.fsck.k9.service.CoreService.wakeLockId";

    private static ConcurrentHashMap<Integer, TracingWakeLock> sWakeLocks =
        new ConcurrentHashMap<Integer, TracingWakeLock>();
    private static AtomicInteger sWakeLockSeq = new AtomicInteger(0);

    /**
     * Threadpool that is used to execute and queue background actions inside the service.
     */
    private ExecutorService mThreadPool = null;

    /**
     * String of the class name used in debug messages.
     */
    private final String className = getClass().getName();

    /**
     * {@code true} if the {@code Service}'s {@link #onDestroy()} method was called. {@code false}
     * otherwise.
     *
     * <p>
     * <strong>Note:</strong>
     * This is used to ignore (expected) {@link RejectedExecutionException}s thrown by
     * {@link ExecutorService#execute(Runnable)} after the service (and with that, the thread pool)
     * was shut down.
     * </p>
     */
    private volatile boolean mShutdown = false;

    /**
     * Controls the auto shutdown mechanism of the service.
     *
     * <p>
     * The default service life-cycle model is that the service should run only as long as a task
     * is running. If a service should behave differently, disable auto shutdown using
     * {@link #setAutoShutdown(boolean)}.
     * </p>
     */
    private boolean mAutoShutdown = true;

    /**
     * This variable is part of the auto shutdown feature and determines whether the service has to
     * be shutdown at the end of the {@link #onStart(Intent, int)} method or not.
     */
    protected boolean mImmediateShutdown = true;


    /**
     * Adds an existing wake lock identified by its registry ID to the specified intent.
     *
     * @param context
     *         A {@link Context} instance. Never {@code null}.
     * @param intent
     *         The {@link Intent} to add the wake lock registy ID as extra to. Never {@code null}.
     * @param wakeLockId
     *         The wake lock registry ID of an existing wake lock or {@code null}.
     * @param createIfNotExists
     *         If {@code wakeLockId} is {@code null} and this parameter is {@code true} a new wake
     *         lock is created, registered, and added to {@code intent}.
     */
    protected static void addWakeLockId(Context context, Intent intent, Integer wakeLockId,
            boolean createIfNotExists) {

        if (wakeLockId != null) {
            intent.putExtra(BootReceiver.WAKE_LOCK_ID, wakeLockId);
            return;
        }

        if (createIfNotExists) {
          addWakeLock(context,intent);
        }
    }

    /**
     * Adds a new wake lock to the specified intent.
     *
     * <p>
     * This will add the wake lock to the central wake lock registry managed by this class.
     * </p>
     *
     * @param context
     *         A {@link Context} instance. Never {@code null}.
     * @param intent
     *         The {@link Intent} to add the wake lock registy ID as extra to. Never {@code null}.
     */
    protected static void addWakeLock(Context context, Intent intent) {
        TracingWakeLock wakeLock = acquireWakeLock(context, "CoreService addWakeLock",
                K9.MAIL_SERVICE_WAKE_LOCK_TIMEOUT);
        Integer tmpWakeLockId = registerWakeLock(wakeLock);
        intent.putExtra(WAKE_LOCK_ID, tmpWakeLockId);
    }

    /**
     * Registers a wake lock with the wake lock registry.
     *
     * @param wakeLock
     *         The {@link TracingWakeLock} instance that should be registered with the wake lock
     *         registry. Never {@code null}.
     *
     * @return The ID that identifies this wake lock in the registry.
     */
    protected static Integer registerWakeLock(TracingWakeLock wakeLock) {
        // Get a new wake lock ID
        Integer tmpWakeLockId = sWakeLockSeq.getAndIncrement();

        // Store the wake lock in the registry
        sWakeLocks.put(tmpWakeLockId, wakeLock);

        return tmpWakeLockId;
    }

    /**
     * Acquires a wake lock.
     *
     * @param context
     *         A {@link Context} instance. Never {@code null}.
     * @param tag
     *         The tag to supply to {@link TracingPowerManager}.
     * @param timeout
     *         The wake lock timeout.
     *
     * @return A new {@link TracingWakeLock} instance.
     */
    protected static TracingWakeLock acquireWakeLock(Context context, String tag, long timeout) {
        TracingPowerManager pm = TracingPowerManager.getPowerManager(context);
        TracingWakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(timeout);
        return wakeLock;
    }

    @Override
    public void onCreate() {
        if (K9.DEBUG) {
            Log.i(K9.LOG_TAG, "CoreService: " + className + ".onCreate()");
        }

        mThreadPool = Executors.newFixedThreadPool(1);  // Must be single threaded
    }

    @Override
    public final int onStartCommand(Intent intent, int flags, int startId) {
        /*
         * When a process is killed due to low memory, it's later restarted and services that were
         * started with START_STICKY are started with the intent being null.
         *
         * For now we just ignore these restart events. This should be fine because all necessary
         * services are started from K9.onCreate() when the Application object is initialized.
         *
         * See issue 3750
         */
        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        // Acquire new wake lock
        TracingWakeLock wakeLock = acquireWakeLock(this, "CoreService onStart",
                K9.MAIL_SERVICE_WAKE_LOCK_TIMEOUT);

        if (K9.DEBUG) {
            Log.i(K9.LOG_TAG, "CoreService: " + className + ".onStart(" + intent + ", " + startId + ")");
        }

        // If we were started by BootReceiver, release the wake lock acquired there.
        int wakeLockId = intent.getIntExtra(BootReceiver.WAKE_LOCK_ID, -1);
        if (wakeLockId != -1) {
            BootReceiver.releaseWakeLock(this, wakeLockId);
        }

        // If we were passed an ID from our own wake lock registry, retrieve that wake lock and
        // release it.
        int coreWakeLockId = intent.getIntExtra(WAKE_LOCK_ID, -1);
        if (coreWakeLockId != -1) {
            if (K9.DEBUG) {
                Log.d(K9.LOG_TAG, "Got core wake lock id " + coreWakeLockId);
            }

            // Remove wake lock from the registry
            TracingWakeLock coreWakeLock = sWakeLocks.remove(coreWakeLockId);

            // Release wake lock
            if (coreWakeLock != null) {
                if (K9.DEBUG) {
                    Log.d(K9.LOG_TAG, "Found core wake lock with id " + coreWakeLockId +
                            ", releasing");
                }
                coreWakeLock.release();
            }
        }

        // Run the actual start-code of the service
        mImmediateShutdown = true;
        int startFlag;
        try {
            startFlag = startService(intent, startId);
        } finally {
            try {
                // Release the wake lock acquired at the start of this method
                wakeLock.release();
            } catch (Exception e) { /* ignore */ }

            try {
                // If there is no outstanding work to be done in a background thread we can stop
                // this service.
                if (mAutoShutdown && mImmediateShutdown && startId != -1) {
                    stopSelf(startId);
                    startFlag = START_NOT_STICKY;
                }
            } catch (Exception e) { /* ignore */ }
        }

        return startFlag;
    }

    /**
     * Execute a task in the background thread.
     *
     * @param context
     *         A {@link Context} instance. Never {@code null}.
     * @param runner
     *         The code to be executed in the background thread.
     * @param wakeLockTime
     *         The timeout for the wake lock that will be acquired by this method.
     * @param startId
     *         The {@code startId} value received in {@link #onStart(Intent, int)} or {@code null}
     *         if you don't want the service to be shut down after {@code runner} has been executed
     *         (e.g. because you need to run another background task).<br>
     *         If this parameter is {@code null} you need to call {@code setAutoShutdown(false)}
     *         otherwise the auto shutdown code will stop the service.
     */
    public void execute(Context context, final Runnable runner, int wakeLockTime,
            final Integer startId) {

        boolean serviceShutdownScheduled = false;
        final boolean autoShutdown = mAutoShutdown;

        // Acquire a new wakelock
        final TracingWakeLock wakeLock = acquireWakeLock(context, "CoreService execute",
                wakeLockTime);

        // Wrap the supplied runner with code to release the wake lock and stop the service if
        // appropriate.
        Runnable myRunner = new Runnable() {
            public void run() {
                try {
                    // Get the sync status
                    boolean oldIsSyncDisabled = MailService.isSyncDisabled();

                    if (K9.DEBUG) {
                        Log.d(K9.LOG_TAG, "CoreService (" + className + ") running Runnable " +
                                runner.hashCode() + " with startId " + startId);
                    }

                    // Run the supplied code
                    runner.run();

                    // If the sync status changed while runner was executing, notify
                    // MessagingController
                    if (MailService.isSyncDisabled() != oldIsSyncDisabled) {
                        MessagingController.getInstance(getApplication()).systemStatusChanged();
                    }
                } finally {
                    // Making absolutely sure stopSelf() will be called
                    try {
                        if (K9.DEBUG) {
                            Log.d(K9.LOG_TAG, "CoreService (" + className + ") completed " +
                                    "Runnable " + runner.hashCode() + " with startId " + startId);
                        }
                        wakeLock.release();
                    } finally {
                        if (autoShutdown && startId != null) {
                            stopSelf(startId);
                        }
                    }
                }
            }
        };

        // TODO: remove this. we never set mThreadPool to null
        if (mThreadPool == null) {
            Log.e(K9.LOG_TAG, "CoreService.execute (" + className + ") called with no thread " +
                    "pool available; running Runnable " + runner.hashCode() +
                    " in calling thread");

            synchronized (this) {
                myRunner.run();
                serviceShutdownScheduled = startId != null;
            }
        } else {
            if (K9.DEBUG) {
                Log.d(K9.LOG_TAG, "CoreService (" + className + ") queueing Runnable " +
                        runner.hashCode() + " with startId " + startId);
            }

            try {
                mThreadPool.execute(myRunner);
                serviceShutdownScheduled = startId != null;
            } catch (RejectedExecutionException e) {
                // Ignore RejectedExecutionException after we shut down the thread pool in
                // onDestroy(). Still, this should not happen!
                if (!mShutdown) {
                    throw e;
                }

                Log.i(K9.LOG_TAG, "CoreService: " + className + " is shutting down, ignoring " +
                        "rejected execution exception: " + e.getMessage());
            }
        }

        mImmediateShutdown = !serviceShutdownScheduled;
    }

    /**
     * Subclasses need to implement this instead of overriding {@link #onStartCommand(Intent, int, int)}.
     *
     * <p>
     * This allows {@link CoreService} to manage the service lifecycle, incl. wake lock management.
     * </p>

     * @param intent
     *         The Intent supplied to {@link Context#startService(Intent)}.
     * @param startId
     *         A unique integer representing this specific request to start. Use with
     *         {@link #stopSelfResult(int)}.
     *
     * @return The return value indicates what semantics the system should use for the service's
     *         current started state. It may be one of the constants associated with the
     *         {@link Service#START_CONTINUATION_MASK} bits.
     */
    public abstract int startService(Intent intent, int startId);

    @Override
    public void onLowMemory() {
        Log.w(K9.LOG_TAG, "CoreService: " + className + ".onLowMemory() - Running low on memory");
    }

    /**
     * Clean up when the service is stopped.
     */
    @Override
    public void onDestroy() {
        if (K9.DEBUG) {
            Log.i(K9.LOG_TAG, "CoreService: " + className + ".onDestroy()");
        }

        // Shut down thread pool
        mShutdown = true;
        mThreadPool.shutdown();
    }

    /**
     * Return whether or not auto shutdown is enabled.
     *
     * @return {@code true} iff auto shutdown is enabled.
     */
    protected boolean isAutoShutdown() {
        return mAutoShutdown;
    }

    /**
     * Enable or disable auto shutdown (enabled by default).
     *
     * @param autoShutdown
     *         {@code true} to enable auto shutdown. {@code false} to disable.
     *
     * @see #mAutoShutdown
     */
    protected void setAutoShutdown(boolean autoShutdown) {
        mAutoShutdown = autoShutdown;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Unused
        return null;
    }
}
