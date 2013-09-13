/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.openintents.openpgp;

import org.openintents.openpgp.IOpenPgpService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class OpenPgpServiceConnection {
    private Context mApplicationContext;

    private IOpenPgpService mService;
    private boolean bound;
    private String cryptoProviderPackageName;

    private static final String TAG = "OpenPgpServiceConnection";

    public OpenPgpServiceConnection(Context context, String cryptoProviderPackageName) {
        mApplicationContext = context.getApplicationContext();
        this.cryptoProviderPackageName = cryptoProviderPackageName;
    }

    public IOpenPgpService getService() {
        return mService;
    }

    private ServiceConnection mCryptoServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IOpenPgpService.Stub.asInterface(service);
            Log.d(TAG, "connected to service");
            bound = true;
        }

        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            Log.d(TAG, "disconnected from service");
            bound = false;
        }
    };

    /**
     * If not already bound, bind!
     * 
     * @return
     */
    public boolean bindToService() {
        if (mService == null && !bound) { // if not already connected
            try {
                Log.d(TAG, "not bound yet");

                Intent serviceIntent = new Intent();
                serviceIntent.setAction(IOpenPgpService.class.getName());
                serviceIntent.setPackage(cryptoProviderPackageName);
                mApplicationContext.bindService(serviceIntent, mCryptoServiceConnection,
                        Context.BIND_AUTO_CREATE);

                return true;
            } catch (Exception e) {
                Log.d(TAG, "Exception", e);
                return false;
            }
        } else { // already connected
            Log.d(TAG, "already bound... ");
            return true;
        }
    }

    public void unbindFromService() {
        mApplicationContext.unbindService(mCryptoServiceConnection);
    }

}
