/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.openintents.openpgp.IOpenPgpService2;

public class OpenPgpServiceConnection {

    // callback interface
    public interface OnBound {
        public void onBound(IOpenPgpService2 service);

        public void onError(Exception e);
    }

    private Context applicationContext;

    private IOpenPgpService2 service;
    private String providerPackageName;

    private OnBound onBoundListener;

    /**
     * Create new connection
     *
     * @param context
     * @param providerPackageName specify package name of OpenPGP provider,
     *                            e.g., "org.sufficientlysecure.keychain"
     */
    public OpenPgpServiceConnection(Context context, String providerPackageName) {
        this.applicationContext = context.getApplicationContext();
        this.providerPackageName = providerPackageName;
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
        this.onBoundListener = onBoundListener;
    }

    public IOpenPgpService2 getService() {
        return service;
    }

    public boolean isBound() {
        return (service != null);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            OpenPgpServiceConnection.this.service = IOpenPgpService2.Stub.asInterface(service);
            if (onBoundListener != null) {
                onBoundListener.onBound(OpenPgpServiceConnection.this.service);
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            service = null;
        }
    };

    /**
     * If not already bound, bind to service!
     *
     * @return
     */
    public void bindToService() {
        // if not already bound...
        if (service == null) {
            try {
                Intent serviceIntent = new Intent(OpenPgpApi.SERVICE_INTENT_2);
                // NOTE: setPackage is very important to restrict the intent to this provider only!
                serviceIntent.setPackage(providerPackageName);
                boolean connect = applicationContext.bindService(serviceIntent, serviceConnection,
                        Context.BIND_AUTO_CREATE);
                if (!connect) {
                    throw new Exception("bindService() returned false!");
                }
            } catch (Exception e) {
                if (onBoundListener != null) {
                    onBoundListener.onError(e);
                }
            }
        } else {
            // already bound, but also inform client about it with callback
            if (onBoundListener != null) {
                onBoundListener.onBound(service);
            }
        }
    }

    public void unbindFromService() {
        applicationContext.unbindService(serviceConnection);
    }

}
