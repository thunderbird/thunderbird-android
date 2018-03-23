package com.fsck.k9.ui.crypto;


import android.app.PendingIntent;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.Lifecycle.Event;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpApi.IOpenPgpCallback;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.openintents.openpgp.util.OpenPgpServiceConnection.OnBound;
import timber.log.Timber;


public class OpenPgpApiManager implements LifecycleObserver {
    private final Context context;
    private final String openPgpProvider;

    private OpenPgpServiceConnection openPgpServiceConnection;
    private OpenPgpApi openPgpApi;
    private PendingIntent pendingUserInteractionIntent;
    private OpenPgpApiManagerCallback callback;
    private CryptoProviderState cryptoProviderState = CryptoProviderState.UNCONFIGURED;

    public OpenPgpApiManager(Context context, Lifecycle lifecycle,
            OpenPgpApiManagerCallback callback, String openPgpProvider) {
        this.context = context;
        this.callback = callback;
        this.openPgpProvider = openPgpProvider;

        lifecycle.addObserver(this);
    }

    @OnLifecycleEvent(Event.ON_CREATE)
    void onLifecycleCreate() {
        setupCryptoProvider();
    }

    @OnLifecycleEvent(Event.ON_START)
    void onLifecycleStart() {
        refreshConnection();
    }

    @OnLifecycleEvent(Event.ON_DESTROY)
    public void onLifecycleDestroy() {
        disconnect();
    }

    private void setupCryptoProvider() {
        boolean providerIsBound = openPgpServiceConnection != null && openPgpServiceConnection.isBound();
        if (providerIsBound) {
            refreshConnection();
            return;
        }

        if (openPgpProvider == null) {
            setCryptoProviderState(CryptoProviderState.UNCONFIGURED);
            return;
        }

        setCryptoProviderState(CryptoProviderState.UNINITIALIZED);
        openPgpServiceConnection = new OpenPgpServiceConnection(context, openPgpProvider, new OnBound() {
            @Override
            public void onBound(IOpenPgpService2 service) {
                openPgpApi = new OpenPgpApi(context, service);
                refreshConnection();
            }

            @Override
            public void onError(Exception e) {
                Timber.e(e, "error connecting to crypto provider!");
                setCryptoProviderState(CryptoProviderState.ERROR);
                callback.onCryptoProviderError(CryptoProviderError.ConnectionFailed);
            }
        });
        refreshConnection();
    }

    public void refreshConnection() {
        boolean isOkStateButLostConnection = cryptoProviderState == CryptoProviderState.OK &&
                (openPgpServiceConnection == null || !openPgpServiceConnection.isBound());
        if (isOkStateButLostConnection) {
            setCryptoProviderState(CryptoProviderState.LOST_CONNECTION);
            pendingUserInteractionIntent = null;
            return;
        }

        if (openPgpServiceConnection == null) {
            setCryptoProviderState(CryptoProviderState.UNCONFIGURED);
            return;
        }

        if (!openPgpServiceConnection.isBound()) {
            pendingUserInteractionIntent = null;
            openPgpServiceConnection.bindToService();
            return;
        }

        if (pendingUserInteractionIntent != null) {
            callback.launchUserInteractionPendingIntent(pendingUserInteractionIntent);
            pendingUserInteractionIntent = null;
            return;
        }

        Intent intent = new Intent(OpenPgpApi.ACTION_CHECK_PERMISSION);
        getOpenPgpApi().executeApiAsync(intent, null, null, new IOpenPgpCallback() {
            @Override
            public void onReturn(Intent result) {
                onPgpPermissionCheckResult(result);
            }
        });
    }

    public void onUserInteractionResult() {
        refreshConnection();
    }

    private void onPgpPermissionCheckResult(Intent result) {
        int resultCode = result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
        switch (resultCode) {
            case OpenPgpApi.RESULT_CODE_SUCCESS:
                setCryptoProviderState(CryptoProviderState.OK);
                break;

            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED:
                pendingUserInteractionIntent = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                setCryptoProviderState(CryptoProviderState.ERROR);
                callback.onCryptoProviderError(CryptoProviderError.UserInteractionRequired);
                break;

            case OpenPgpApi.RESULT_CODE_ERROR:
            default:
                if (result.hasExtra(OpenPgpApi.RESULT_ERROR)) {
                    OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                    handleOpenPgpError(error);
                } else {
                    setCryptoProviderState(CryptoProviderState.ERROR);
                    callback.onCryptoProviderError(CryptoProviderError.ConnectionFailed);
                }
                break;
        }
    }

    private void setCryptoProviderState(CryptoProviderState state) {
        boolean statusChanged = cryptoProviderState != state;
        if (statusChanged) {
            cryptoProviderState = state;
            callback.onCryptoStatusChanged();
        }
    }

    private void handleOpenPgpError(@Nullable OpenPgpError error) {
        Timber.e("OpenPGP Api error: %s", error);

        if (error != null && error.getErrorId() == OpenPgpError.INCOMPATIBLE_API_VERSIONS) {
            callback.onCryptoProviderError(CryptoProviderError.VersionIncompatible);
            setCryptoProviderState(CryptoProviderState.UNCONFIGURED);
        } else {
            callback.onCryptoProviderError(CryptoProviderError.ConnectionFailed);
            setCryptoProviderState(CryptoProviderState.ERROR);
        }
    }

    private void disconnect() {
        openPgpApi = null;
        if (openPgpServiceConnection != null) {
            openPgpServiceConnection.unbindFromService();
        }
        openPgpServiceConnection = null;
    }

    public OpenPgpApi getOpenPgpApi() {
        if (openPgpServiceConnection == null || !openPgpServiceConnection.isBound()) {
            Timber.e("obtained openpgpapi object, but service is not bound! inconsistent state?");
        }
        return openPgpApi;
    }

    public CryptoProviderState getCryptoProviderState() {
        return cryptoProviderState;
    }

    public enum CryptoProviderState {
        UNCONFIGURED,
        UNINITIALIZED,
        LOST_CONNECTION,
        ERROR,
        OK
    }

    public enum CryptoProviderError {
        ConnectionFailed, VersionIncompatible, UserInteractionRequired
    }

    public interface OpenPgpApiManagerCallback {
        void launchUserInteractionPendingIntent(PendingIntent pendingIntent);
        void onCryptoStatusChanged();
        void onCryptoProviderError(CryptoProviderError error);
    }
}
