package com.fsck.k9.activity.misc;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

/**
 * Extends {@link AsyncTask} with methods to attach and detach an {@link Activity}.
 *
 * <p>
 * This is necessary to properly handle configuration changes that will restart an activity.
 * </p><p>
 * <strong>Note:</strong>
 * Implementing classes need to make sure they have no reference to the {@code Activity} instance
 * that created the instance of that class. So if it's implemented as inner class, it needs to be
 * {@code static}.
 * </p>
 *
 * @param <Params>
 *         see {@link AsyncTask}
 * @param <Progress>
 *         see {@link AsyncTask}
 * @param <Result>
 *         see {@link AsyncTask}
 *
 * @see #restore(Activity)
 * @see #retain()
 */
public abstract class ExtendedAsyncTask<Params, Progress, Result>
        extends AsyncTask<Params, Progress, Result> implements NonConfigurationInstance {
    protected Activity mActivity;
    protected Context mContext;
    protected ProgressDialog mProgressDialog;

    protected ExtendedAsyncTask(Activity activity) {
        mActivity = activity;
        mContext = activity.getApplicationContext();
    }

    /**
     * Connect this {@link AsyncTask} to a new {@link Activity} instance after the activity
     * was restarted due to a configuration change.
     *
     * <p>
     * This also creates a new progress dialog that is bound to the new activity.
     * </p>
     *
     * @param activity
     *         The new {@code Activity} instance. Never {@code null}.
     */
    @Override
    public void restore(Activity activity) {
        mActivity = activity;
        showProgressDialog();
    }

    /**
     * Detach this {@link AsyncTask} from the {@link Activity} it was bound to.
     *
     * <p>
     * This needs to be called when the current activity is being destroyed during an activity
     * restart due to a configuration change.<br/>
     * We also have to destroy the progress dialog because it's bound to the activity that's
     * being destroyed.
     * </p>
     *
     * @return {@code true} if this instance should be retained; {@code false} otherwise.
     *
     * @see Activity#onRetainNonConfigurationInstance()
     */
    @Override
    public boolean retain() {
        boolean retain = false;
        if (mProgressDialog != null) {
            removeProgressDialog();
            retain = true;
        }
        mActivity = null;

        return retain;
    }

    /**
     * Creates a {@link ProgressDialog} that is shown while the background thread is running.
     *
     * <p>
     * This needs to store a {@code ProgressDialog} instance in {@link #mProgressDialog} or
     * override {@link #removeProgressDialog()}.
     * </p>
     */
    protected abstract void showProgressDialog();

    protected void removeProgressDialog() {
        mProgressDialog.dismiss();
        mProgressDialog = null;
    }

    /**
     * This default implementation only creates a progress dialog.
     *
     * <p>
     * <strong>Important:</strong>
     * Be sure to call {@link #removeProgressDialog()} in {@link AsyncTask#onPostExecute(Object)}.
     * </p>
     */
    @Override
    protected void onPreExecute() {
        showProgressDialog();
    }
}
