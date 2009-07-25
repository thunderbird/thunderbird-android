/*
 * Copyright (C) 2007 The Android Open Source Project
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

/**
 * package-level logging flag
 */

package android.net.http;

import android.os.SystemClock;

import android.util.Log;
import android.util.Config;

/**
 * {@hide}
 */
class HttpLog {
    private final static String LOGTAG = "http";

    private static final boolean DEBUG = false;
    static final boolean LOGV = DEBUG ? Config.LOGD : Config.LOGV;

    static void v(String logMe) {
        Log.v(LOGTAG, SystemClock.uptimeMillis() + " " + Thread.currentThread().getName() + " " + logMe);
    }

    static void e(String logMe) {
        Log.e(LOGTAG, logMe);
    }
}
