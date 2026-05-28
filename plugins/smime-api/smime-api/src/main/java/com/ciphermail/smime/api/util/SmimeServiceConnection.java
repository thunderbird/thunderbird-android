package com.ciphermail.smime.api.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.ciphermail.smime.api.ISmimeService;

/**
 * Manages binding to the CipherMail S/MIME service.
 * Mirrors OpenPgpServiceConnection so Thunderbird's integration layer can
 * follow the same lifecycle pattern for both crypto providers.
 */
public class SmimeServiceConnection {

    /**
     * Listener fired on successful bind or bind failure. Methods run on the
     * main thread (delivered by {@link android.content.ServiceConnection}).
     */
    public interface OnBound {
        void onBound(ISmimeService service);
        void onError(Exception e);
    }

    private final Context mApplicationContext;
    private final String mProviderPackageName;
    private final OnBound mOnBoundListener;

    private ISmimeService mService;

    /** Build a connection without a listener — use {@link #isBound()} to poll. */
    public SmimeServiceConnection(Context context, String providerPackageName) {
        this(context, providerPackageName, null);
    }

    /**
     * @param context             Any context; the application context is kept internally.
     * @param providerPackageName Package id of the S/MIME provider (e.g.
     *                            {@code "com.ciphermail.android"} or
     *                            {@code "com.ciphermail.android.debug"}).
     * @param onBoundListener     Optional bind-result callback.
     */
    public SmimeServiceConnection(Context context, String providerPackageName, OnBound onBoundListener) {
        this.mApplicationContext = context.getApplicationContext();
        this.mProviderPackageName = providerPackageName;
        this.mOnBoundListener = onBoundListener;
    }

    /** @return the bound {@link ISmimeService}, or {@code null} if not yet bound. */
    public ISmimeService getService() { return mService; }

    /** @return {@code true} once {@code onServiceConnected} has fired. */
    public boolean isBound() { return mService != null; }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ISmimeService.Stub.asInterface(service);
            if (mOnBoundListener != null) {
                mOnBoundListener.onBound(mService);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    /**
     * Bind to the provider's S/MIME service. Idempotent: if already bound,
     * fires {@code onBound} again on the listener without rebinding.
     * On failure the listener's {@code onError} is invoked synchronously.
     */
    public void bindToService() {
        if (mService != null) {
            if (mOnBoundListener != null) mOnBoundListener.onBound(mService);
            return;
        }
        try {
            Intent serviceIntent = new Intent(SmimeApi.SERVICE_INTENT);
            serviceIntent.setPackage(mProviderPackageName);
            boolean connected = mApplicationContext.bindService(
                    serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
            if (!connected) {
                throw new Exception("bindService() returned false for package: " + mProviderPackageName);
            }
        } catch (Exception e) {
            if (mOnBoundListener != null) mOnBoundListener.onError(e);
        }
    }

    /**
     * Unbind from the provider. Safe to call multiple times; safe to call
     * before {@link #bindToService()} has succeeded only if the connection
     * has been at least registered (otherwise Android throws).
     */
    public void unbindFromService() {
        mApplicationContext.unbindService(mServiceConnection);
        mService = null;
    }
}
