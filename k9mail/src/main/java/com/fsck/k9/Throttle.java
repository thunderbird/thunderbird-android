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

package com.fsck.k9;


import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;

import timber.log.Timber;


/**
 * This class used to "throttle" a flow of events.
 *
 * When {@link #onEvent()} is called, it calls the callback in a certain timeout later.
 * Initially {@link #minTimeout} is used as the timeout, but if it gets multiple {@link #onEvent}
 * calls in a certain amount of time, it extends the timeout, until it reaches {@link #maxTimeout}.
 *
 * This class is primarily used to throttle content changed events.
 */
public class Throttle {
    private static final int TIMEOUT_EXTEND_INTERVAL = 500;

    private static Timer TIMER = new Timer();

    private final Clock clock;
    private final Timer timer;

    private final String name;
    private final Handler handler;
    private final Runnable callback;

    private final int minTimeout;
    private final int maxTimeout;
    private int currentTimeout;

    /** When {@link #onEvent()} was last called. */
    private long lastEventTime;

    private MyTimerTask runningTimerTask;

    /** Constructor that takes custom timeout */
    public Throttle(String name, Runnable callback, Handler handler,int minTimeout,
            int maxTimeout) {
        this(name, callback, handler, minTimeout, maxTimeout, Clock.INSTANCE, TIMER);
    }

    /** Constructor for tests */
    private Throttle(String name, Runnable callback, Handler handler, int minTimeout,
            int maxTimeout, Clock clock, Timer timer) {
        if (maxTimeout < minTimeout) {
            throw new IllegalArgumentException();
        }
        this.name = name;
        this.callback = callback;
        this.clock = clock;
        this.timer = timer;
        this.handler = handler;
        this.minTimeout = minTimeout;
        this.maxTimeout = maxTimeout;
        currentTimeout = this.minTimeout;
    }

    private boolean isCallbackScheduled() {
        return runningTimerTask != null;
    }

    public void cancelScheduledCallback() {
        if (runningTimerTask != null) {
            Timber.d("Throttle: [%s] Canceling scheduled callback", name);
            runningTimerTask.cancel();
            runningTimerTask = null;
        }
    }

    private void updateTimeout() {
        final long now = clock.getTime();
        if ((now - lastEventTime) <= TIMEOUT_EXTEND_INTERVAL) {
            currentTimeout *= 2;
            if (currentTimeout >= maxTimeout) {
                currentTimeout = maxTimeout;
            }
            Timber.d("Throttle: [%s] Timeout extended %d", name, currentTimeout);
        } else {
            currentTimeout = minTimeout;
            Timber.d("Throttle: [%s] Timeout reset to %d", name, currentTimeout);
        }

        lastEventTime = now;
    }

    public void onEvent() {
        Timber.d("Throttle: [%s] onEvent", name);

        updateTimeout();

        if (isCallbackScheduled()) {
            Timber.d("Throttle: [%s]     callback already scheduled", name);
        } else {
            Timber.d("Throttle: [%s]     scheduling callback", name);
            runningTimerTask = new MyTimerTask();
            timer.schedule(runningTimerTask, currentTimeout);
        }
    }

    /**
     * Timer task called on timeout,
     */
    private class MyTimerTask extends TimerTask {
        private boolean mCanceled;

        @Override
        public void run() {
            handler.post(new HandlerRunnable());
        }

        @Override
        public boolean cancel() {
            mCanceled = true;
            return super.cancel();
        }

        private class HandlerRunnable implements Runnable {
            @Override
            public void run() {
                runningTimerTask = null;
                if (!mCanceled) { // This check has to be done on the UI thread.
                    Timber.d("Throttle: [%s] Kicking callback", name);
                    callback.run();
                }
            }
        }
    }
}
