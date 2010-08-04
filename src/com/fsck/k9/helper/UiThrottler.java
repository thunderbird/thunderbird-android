package com.fsck.k9.helper;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.os.AsyncTask;

/**
 * Helper class to manage UI interation throttling in order not to stress the UI
 * thread with fast and repeated update requests. Throttling is achieved by
 * <strong>ignoring</strong> extra processing attemps: there is no stack
 * involved and will result in less processing executions than initially
 * attempted.
 * 
 * <p>
 * A throttled action will only be executed after a cool-down sleep time which
 * begins right after the last execution finished.
 * </p>
 * 
 * <p>
 * You must provide an {@link Activity} and a {@link Callable}.
 * </p>
 * 
 * <p>
 * Based on {@link AsyncTask}.
 * </p>
 * 
 * @param <Result>
 * @see AsyncTask
 */
public class UiThrottler<Result>
{

    public static final long DEFAULT_COOLDOWN_DURATION = 1000L;

    private static final Runnable NO_OP = new Runnable()
    {
        @Override
        public void run()
        {
            // no-op
        }
    };

    private final class ProcessingTask extends AsyncTask<Void, Void, Result>
    {
        @Override
        protected void onPreExecute()
        {
            preExecute.run();
        }

        @Override
        protected Result doInBackground(final Void... params)
        {
            // we're going to begin processing, any data that came until now
            // will be processed, discarding dirty state
            processingPending = false;
            try
            {
                return processing.call();
            }
            catch (Exception e)
            {
                // XXX improve exception handling
                throw new IllegalStateException(e);
            }
        }

        // no support for progress: "processing" isn't AsyncTask aware

        @Override
        protected void onPostExecute(final Result result)
        {
            // back on UI thread, can do the actual dataset update
            postExecute.run();

            // trigger cool-down
            scheduledExecutorService.schedule(coolDown, coolDownDuration, TimeUnit.MILLISECONDS);
        }

        @Override
        protected void onCancelled()
        {
            cancelled.run();
        }
    }

    /**
     * Cool-down end internal processing.
     */
    private final Runnable coolDown = new Runnable()
    {
        @Override
        public void run()
        {
            processingInProgress.set(false);
            if (processingPending)
            {
                activity.runOnUiThread(newAttempt);
            }
        }
    };

    /**
     * Trigger a new attempt.
     */
    private final Runnable newAttempt = new Runnable()
    {
        @Override
        public void run()
        {
            attempt();
        }
    };

    /**
     * Indicate whether a processing is currently occuring.
     */
    private final AtomicBoolean processingInProgress = new AtomicBoolean(false);

    /**
     * Used for UI threading purpose
     */
    private Activity activity;

    private ScheduledExecutorService scheduledExecutorService;

    /**
     * Indicate whether a processing is pending and should be triggered when
     * possible.
     */
    private boolean processingPending = false;

    /**
     * Main processing, which will be executed in a background thread (as
     * opposed to the UI thread).
     * 
     * @see AsyncTask#doInBackground(Params)
     */
    private Callable<Result> processing;

    /**
     * @see AsyncTask#onPreExecute()
     */
    private Runnable preExecute = NO_OP;

    /**
     * @see AsyncTask#onPostExecute(Result)
     */
    private Runnable postExecute = NO_OP;

    /**
     * @see AsyncTask#onCancelled()
     */
    private Runnable cancelled = NO_OP;

    /**
     * Cool-down duration in milliseconds before beginning the pending
     * processing (if any)
     */
    private long coolDownDuration = DEFAULT_COOLDOWN_DURATION;

    public UiThrottler()
    {
        this(null, null, null);
    }

    public UiThrottler(final Activity activity, final Callable<Result> processing)
    {
        this(activity, processing, DEFAULT_COOLDOWN_DURATION, NO_OP, NO_OP, NO_OP, Executors
                .newScheduledThreadPool(1));
    }

    // consider using this constructor instead of the one above as this one
    // doesn't instantiate a new ScheduledExecutorService on its own
    public UiThrottler(final Activity activity, final Callable<Result> processing,
            final ScheduledExecutorService scheduledExecutorService)
    {
        this(activity, processing, DEFAULT_COOLDOWN_DURATION, NO_OP, NO_OP, NO_OP,
                scheduledExecutorService);
    }

    public UiThrottler(final Activity activity, final Callable<Result> processing,
            final long coolDownDuration, final Runnable preExecute, final Runnable postExecute,
            final Runnable cancelled, final ScheduledExecutorService scheduledExecutorService)
    {
        this.activity = activity;
        this.processing = processing;
        this.coolDownDuration = coolDownDuration;
        this.preExecute = preExecute;
        this.postExecute = postExecute;
        this.cancelled = cancelled;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    /**
     * Attempt to execute the {@link #setProcessing(Callable) configured
     * processing}.
     * 
     * <p>
     * This method has to be called from the UI thread.
     * </p>
     * 
     * @return The resulting {@link AsyncTask} instance if the attempt wasn't
     *         throttled, <code>null</code> otherwise.
     */
    public AsyncTask<Void, Void, Result> attempt()
    {
        processingPending = true;
        if (processingInProgress.compareAndSet(false, true))
        {
            return new ProcessingTask().execute();
        }
        return null;
    }

    public Activity getActivity()
    {
        return activity;
    }

    public void setActivity(Activity activity)
    {
        this.activity = activity;
    }

    public ScheduledExecutorService getScheduledExecutorService()
    {
        return scheduledExecutorService;
    }

    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService)
    {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public boolean isProcessingPending()
    {
        return processingPending;
    }

    public void setProcessingPending(boolean processingPending)
    {
        this.processingPending = processingPending;
    }

    public Callable<Result> getProcessing()
    {
        return processing;
    }

    public void setProcessing(Callable<Result> processing)
    {
        this.processing = processing;
    }

    public Runnable getPreExecute()
    {
        return preExecute;
    }

    public void setPreExecute(Runnable preExecute)
    {
        this.preExecute = preExecute;
    }

    public Runnable getPostExecute()
    {
        return postExecute;
    }

    public void setPostExecute(Runnable postExecute)
    {
        this.postExecute = postExecute;
    }

    public Runnable getCancelled()
    {
        return cancelled;
    }

    public void setCancelled(Runnable cancelled)
    {
        this.cancelled = cancelled;
    }

    public long getCoolDownDuration()
    {
        return coolDownDuration;
    }

    public void setCoolDownDuration(long coolDownDuration)
    {
        this.coolDownDuration = coolDownDuration;
    }

}
