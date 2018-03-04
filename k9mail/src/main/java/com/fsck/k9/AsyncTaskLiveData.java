package com.fsck.k9;


import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.CancellationSignal;
import android.support.v4.os.OperationCanceledException;


public abstract class AsyncTaskLiveData<T> extends LiveData<T> {
    @NonNull
    private final Context context;
    private Uri observedUri;

    @NonNull
    private final ForceLoadContentObserver observer;

    @Nullable
    private CancellationSignal cancellationSignal;

    protected AsyncTaskLiveData(@NonNull Context context, @Nullable Uri observedUri) {
        super();
        this.context = context;
        this.observedUri = observedUri;
        this.observer = new ForceLoadContentObserver();
    }

    protected abstract T asyncLoadData();

    private void loadDataInBackground() {
        new AsyncTask<Void, Void, T>() {
            @Override
            protected T doInBackground(Void... params) {
                try {
                    synchronized (AsyncTaskLiveData.this) {
                        cancellationSignal = new CancellationSignal();
                    }
                    try {
                        return asyncLoadData();
                    } finally {
                        synchronized (AsyncTaskLiveData.this) {
                            cancellationSignal = null;
                        }
                    }
                } catch (OperationCanceledException e) {
                    if (hasActiveObservers()) {
                        throw e;
                    }
                    return null;
                }
            }

            @Override
            protected void onPostExecute(T value) {
                setValue(value);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onActive() {
        T value = getValue();
        if (value == null) {
            loadDataInBackground();
        }

        if (observedUri != null) {
            getContext().getContentResolver().registerContentObserver(observedUri, true, observer);
        }
    }

    @Override
    protected void onInactive() {
        synchronized (AsyncTaskLiveData.this) {
            if (cancellationSignal != null) {
                cancellationSignal.cancel();
            }
        }

        if (observedUri != null) {
            getContext().getContentResolver().registerContentObserver(observedUri, true, observer);
        }
    }

    @NonNull
    public Context getContext() {
        return context;
    }

    public final class ForceLoadContentObserver extends ContentObserver {

        ForceLoadContentObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            loadDataInBackground();
        }
    }

}