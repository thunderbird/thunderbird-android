/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.email;

import com.android.emailcommon.Logging;

import android.os.Handler;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * This class used to "throttle" a flow of events.
 *
 * When {@link #onEvent()} is called, it calls the callback in a certain timeout later.
 * Initially {@link #mMinTimeout} is used as the timeout, but if it gets multiple {@link #onEvent}
 * calls in a certain amount of time, it extends the timeout, until it reaches {@link #mMaxTimeout}.
 *
 * This class is primarily used to throttle content changed events.
 */
public class Throttle {
    public static final boolean DEBUG = false; // Don't submit with true

    public static final int DEFAULT_MIN_TIMEOUT = 150;
    public static final int DEFAULT_MAX_TIMEOUT = 2500;
    /* package */ static final int TIMEOUT_EXTEND_INTERVAL = 500;

    private static Timer TIMER = new Timer();

    private final Clock mClock;
    private final Timer mTimer;

    /** Name of the instance.  Only for logging. */
    private final String mName;

    /** Handler for UI thread. */
    private final Handler mHandler;

    /** Callback to be called */
    private final Runnable mCallback;

    /** Minimum (default) timeout, in milliseconds.  */
    private final int mMinTimeout;

    /** Max timeout, in milliseconds.  */
    private final int mMaxTimeout;

    /** Current timeout, in milliseconds. */
    private int mTimeout;

    /** When {@link #onEvent()} was last called. */
    private long mLastEventTime;

    private MyTimerTask mRunningTimerTask;

    /** Constructor with default timeout */
    public Throttle(String name, Runnable callback, Handler handler) {
        this(name, callback, handler, DEFAULT_MIN_TIMEOUT, DEFAULT_MAX_TIMEOUT);
    }

    /** Constructor that takes custom timeout */
    public Throttle(String name, Runnable callback, Handler handler,int minTimeout,
            int maxTimeout) {
        this(name, callback, handler, minTimeout, maxTimeout, Clock.INSTANCE, TIMER);
    }

    /** Constructor for tests */
    /* package */ Throttle(String name, Runnable callback, Handler handler,int minTimeout,
            int maxTimeout, Clock clock, Timer timer) {
        if (maxTimeout < minTimeout) {
            throw new IllegalArgumentException();
        }
        mName = name;
        mCallback = callback;
        mClock = clock;
        mTimer = timer;
        mHandler = handler;
        mMinTimeout = minTimeout;
        mMaxTimeout = maxTimeout;
        mTimeout = mMinTimeout;
    }

    private void debugLog(String message) {
        Log.d(Logging.LOG_TAG, "Throttle: [" + mName + "] " + message);
    }

    private boolean isCallbackScheduled() {
        return mRunningTimerTask != null;
    }

    public void cancelScheduledCallback() {
        if (mRunningTimerTask != null) {
            if (DEBUG) debugLog("Canceling scheduled callback");
            mRunningTimerTask.cancel();
            mRunningTimerTask = null;
        }
    }

    /* package */ void updateTimeout() {
        final long now = mClock.getTime();
        if ((now - mLastEventTime) <= TIMEOUT_EXTEND_INTERVAL) {
            mTimeout *= 2;
            if (mTimeout >= mMaxTimeout) {
                mTimeout = mMaxTimeout;
            }
            if (DEBUG) debugLog("Timeout extended " + mTimeout);
        } else {
            mTimeout = mMinTimeout;
            if (DEBUG) debugLog("Timeout reset to " + mTimeout);
        }

        mLastEventTime = now;
    }

    public void onEvent() {
        if (DEBUG) debugLog("onEvent");

        updateTimeout();

        if (isCallbackScheduled()) {
            if (DEBUG) debugLog("    callback already scheduled");
        } else {
            if (DEBUG) debugLog("    scheduling callback");
            mRunningTimerTask = new MyTimerTask();
            mTimer.schedule(mRunningTimerTask, mTimeout);
        }
    }

    /**
     * Timer task called on timeout,
     */
    private class MyTimerTask extends TimerTask {
        private boolean mCanceled;

        @Override
        public void run() {
            mHandler.post(new HandlerRunnable());
        }

        @Override
        public boolean cancel() {
            mCanceled = true;
            return super.cancel();
        }

        private class HandlerRunnable implements Runnable {
            @Override
            public void run() {
                mRunningTimerTask = null;
                if (!mCanceled) { // This check has to be done on the UI thread.
                    if (DEBUG) debugLog("Kicking callback");
                    mCallback.run();
                }
            }
        }
    }

    /* package */ int getTimeoutForTest() {
        return mTimeout;
    }

    /* package */ long getLastEventTimeForTest() {
        return mLastEventTime;
    }
}
