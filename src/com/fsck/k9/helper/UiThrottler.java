package com.fsck.k9.helper;

import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.fsck.k9.K9;

/**
 * Helper class to manage UI interation throttling in order not to stress the UI
 * thread with fast and repeated update requests. Throttling is achieved by
 * <strong>ignoring</strong> extra processing attemps: there is no stack
 * involved and may result in less processing executions than initially
 * attempted.
 * 
 * <p>
 * The last attempt will always trigger an immediate or delayed processing
 * execution (unless Executor service is shutdown).
 * </p>
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
            mPreExecute.run();
        }

        @Override
        protected Result doInBackground(final Void... params)
        {
            // we're going to begin processing, any data that came until now
            // will be processed, discarding dirty state
            mProcessingPending = false;
            try
            {
                return mProcessing.call();
            }
            catch (Exception e)
            {
                if (mSwallowExceptions)
                {
                    Log.e(K9.LOG_TAG, "UiThrottler: processing task threw an exception! Swallowed!", e);
                    return null;
                }
                // throwing an exception here will result in the application crashing!
                throw new IllegalStateException("UiThrottler: processing task threw an exception",
                        e);
            }
        }

        // no support for progress: "processing" isn't AsyncTask aware

        @Override
        protected void onPostExecute(final Result result)
        {
            // back on UI thread, can do the actual dataset update
            mPostExecute.run();

            try
            {
                // trigger cool-down
                mScheduledExecutorService
                        .schedule(mCoolDown, mCoolDownDuration, TimeUnit.MILLISECONDS);
            }
            catch (RejectedExecutionException e)
            {
                // happens when executor service is shut down before processing completes
                Log.v(K9.LOG_TAG,
                        "UiThrottler: Caller must have shut down Executor during processing (happens when integrating with Activity lifecycle). Stopping cool-down and ignoring any potential pending process.",
                        e);
            }
        }

        @Override
        protected void onCancelled()
        {
            mCancelled.run();
        }
    }

    /**
     * Cool-down end internal processing.
     */
    private final Runnable mCoolDown = new Runnable()
    {
        @Override
        public void run()
        {
            mProcessingInProgress.set(false);
            if (mProcessingPending)
            {
                mActivity.runOnUiThread(mNewAttempt);
            }
            else
            {
                mActivity.runOnUiThread(mCompleted);
            }
        }
    };

    /**
     * Trigger a new attempt.
     */
    private final Runnable mNewAttempt = new Runnable()
    {
        @Override
        public void run()
        {
            // we're in the UI thread
            attempt();
        }
    };

    /**
     * Indicate whether a processing is currently occuring.
     */
    private final AtomicBoolean mProcessingInProgress = new AtomicBoolean(false);

    /**
     * Used for UI threading purpose
     */
    private Activity mActivity;

    private ScheduledExecutorService mScheduledExecutorService;

    /**
     * Indicate whether a processing is pending and should be triggered when
     * possible.
     */
    private boolean mProcessingPending = false;

    /**
     * Main processing, which will be executed in a background thread (as
     * opposed to the UI thread).
     * 
     * @see AsyncTask#doInBackground(Params)
     */
    private Callable<Result> mProcessing;

    /**
     * @see AsyncTask#onPreExecute()
     */
    private Runnable mPreExecute = NO_OP;

    /**
     * @see AsyncTask#onPostExecute(Result)
     */
    private Runnable mPostExecute = NO_OP;

    /**
     * @see AsyncTask#onCancelled()
     */
    private Runnable mCancelled = NO_OP;

    /**
     * Executed after cool-down period if no processing task is pending
     */
    private Runnable mCompleted = NO_OP;

    /**
     * Cool-down duration in milliseconds before beginning the pending
     * processing (if any)
     */
    private long mCoolDownDuration = DEFAULT_COOLDOWN_DURATION;

    private boolean mSwallowExceptions = false;

    public UiThrottler()
    {
        this(null, null, null);
    }

    public UiThrottler(final Activity activity, final Callable<Result> processing,
            final ScheduledExecutorService scheduledExecutorService)
    {
        this.mActivity = activity;
        this.mProcessing = processing;
        this.mScheduledExecutorService = scheduledExecutorService;
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
        mProcessingPending = true;
        if (mProcessingInProgress.compareAndSet(false, true))
        {
            return new ProcessingTask().execute();
        }
        return null;
    }

    public Activity getActivity()
    {
        return mActivity;
    }

    public void setActivity(Activity activity)
    {
        this.mActivity = activity;
    }

    public ScheduledExecutorService getScheduledExecutorService()
    {
        return mScheduledExecutorService;
    }

    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService)
    {
        this.mScheduledExecutorService = scheduledExecutorService;
    }

    public Callable<Result> getProcessing()
    {
        return mProcessing;
    }

    public void setProcessing(Callable<Result> processing)
    {
        this.mProcessing = processing;
    }

    public Runnable getPreExecute()
    {
        return mPreExecute;
    }

    public void setPreExecute(Runnable preExecute)
    {
        this.mPreExecute = preExecute;
    }

    public Runnable getPostExecute()
    {
        return mPostExecute;
    }

    public void setPostExecute(Runnable postExecute)
    {
        this.mPostExecute = postExecute;
    }

    public Runnable getCancelled()
    {
        return mCancelled;
    }

    public void setCancelled(Runnable cancelled)
    {
        this.mCancelled = cancelled;
    }

    public long getCoolDownDuration()
    {
        return mCoolDownDuration;
    }

    public void setCoolDownDuration(long coolDownDuration)
    {
        this.mCoolDownDuration = coolDownDuration;
    }

    public boolean isSwallowExceptions()
    {
        return mSwallowExceptions;
    }

    public void setSwallowExceptions(boolean swallowExceptions)
    {
        this.mSwallowExceptions = swallowExceptions;
    }

    public Runnable getCompleted()
    {
        return mCompleted;
    }

    public void setCompleted(Runnable completed)
    {
        this.mCompleted = completed;
    }

}
