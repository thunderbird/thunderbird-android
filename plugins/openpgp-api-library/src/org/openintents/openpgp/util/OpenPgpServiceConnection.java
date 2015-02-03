/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.openintents.openpgp.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.openintents.openpgp.IOpenPgpService;

public class OpenPgpServiceConnection {

    // callback interface
    public interface OnBound {
        public void onBound(IOpenPgpService service);

        public void onError(Exception e);
    }

    private Context mApplicationContext;

    private IOpenPgpService mService;
    private String mProviderPackageName;

    private OnBound mOnBoundListener;

    /**
     * Create new connection
     *
     * @param context
     * @param providerPackageName specify package name of OpenPGP provider,
     *                            e.g., "org.sufficientlysecure.keychain"
     */
    public OpenPgpServiceConnection(Context context, String providerPackageName) {
        this.mApplicationContext = context.getApplicationContext();
        this.mProviderPackageName = providerPackageName;
    }

    /**
     * Create new connection with callback
     *
     * @param context
     * @param providerPackageName specify package name of OpenPGP provider,
     *                            e.g., "org.sufficientlysecure.keychain"
     * @param onBoundListener     callback, executed when connection to service has been established
     */
    public OpenPgpServiceConnection(Context context, String providerPackageName,
                                    OnBound onBoundListener) {
        this(context, providerPackageName);
        this.mOnBoundListener = onBoundListener;
    }

    public IOpenPgpService getService() {
        return mService;
    }

    public boolean isBound() {
        return (mService != null);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IOpenPgpService.Stub.asInterface(service);
            if (mOnBoundListener != null) {
                mOnBoundListener.onBound(mService);
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    /**
     * If not already bound, bind to service!
     *
     * @return
     */
    public void bindToService() {
        // if not already bound...
        if (mService == null) {
            try {
                Intent serviceIntent = new Intent(OpenPgpApi.SERVICE_INTENT);
                // NOTE: setPackage is very important to restrict the intent to this provider only!
                serviceIntent.setPackage(mProviderPackageName);
                boolean connect = mApplicationContext.bindService(serviceIntent, mServiceConnection,
                        Context.BIND_AUTO_CREATE);
                if (!connect) {
                    throw new Exception("bindService() returned false!");
                }
            } catch (Exception e) {
                if (mOnBoundListener != null) {
                    mOnBoundListener.onError(e);
                }
            }
        } else {
            // already bound, but also inform client about it with callback
            if (mOnBoundListener != null) {
                mOnBoundListener.onBound(mService);
            }
        }
    }

    public void unbindFromService() {
        mApplicationContext.unbindService(mServiceConnection);
    }

}
