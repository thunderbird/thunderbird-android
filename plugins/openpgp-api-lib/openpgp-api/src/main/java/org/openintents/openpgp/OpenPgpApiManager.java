package org.openintents.openpgp;


import android.app.PendingIntent;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.Lifecycle.Event;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

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
    private OpenPgpProviderState openPgpProviderState = OpenPgpProviderState.UNCONFIGURED;

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
            setOpenPgpProviderState(OpenPgpProviderState.UNCONFIGURED);
            return;
        }

        setOpenPgpProviderState(OpenPgpProviderState.UNINITIALIZED);
        openPgpServiceConnection = new OpenPgpServiceConnection(context, openPgpProvider, new OnBound() {
            @Override
            public void onBound(IOpenPgpService2 service) {
                openPgpApi = new OpenPgpApi(context, service);
                refreshConnection();
            }

            @Override
            public void onError(Exception e) {
                Timber.e(e, "error connecting to crypto provider!");
                setOpenPgpProviderState(OpenPgpProviderState.ERROR);
                callback.onOpenPgpProviderError(OpenPgpProviderError.ConnectionFailed);
            }
        });
        refreshConnection();
    }

    public void refreshConnection() {
        boolean isOkStateButLostConnection = openPgpProviderState == OpenPgpProviderState.OK &&
                (openPgpServiceConnection == null || !openPgpServiceConnection.isBound());
        if (isOkStateButLostConnection) {
            setOpenPgpProviderState(OpenPgpProviderState.LOST_CONNECTION);
            pendingUserInteractionIntent = null;
            return;
        }

        if (openPgpServiceConnection == null) {
            setOpenPgpProviderState(OpenPgpProviderState.UNCONFIGURED);
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
                setOpenPgpProviderState(OpenPgpProviderState.OK);
                break;

            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED:
                pendingUserInteractionIntent = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                setOpenPgpProviderState(OpenPgpProviderState.ERROR);
                callback.onOpenPgpProviderError(OpenPgpProviderError.UserInteractionRequired);
                break;

            case OpenPgpApi.RESULT_CODE_ERROR:
            default:
                if (result.hasExtra(OpenPgpApi.RESULT_ERROR)) {
                    OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                    handleOpenPgpError(error);
                } else {
                    setOpenPgpProviderState(OpenPgpProviderState.ERROR);
                    callback.onOpenPgpProviderError(OpenPgpProviderError.ConnectionFailed);
                }
                break;
        }
    }

    private void setOpenPgpProviderState(OpenPgpProviderState state) {
        boolean statusChanged = openPgpProviderState != state;
        if (statusChanged) {
            openPgpProviderState = state;
            callback.onOpenPgpProviderStatusChanged();
        }
    }

    private void handleOpenPgpError(@Nullable OpenPgpError error) {
        Timber.e("OpenPGP Api error: %s", error);

        if (error != null && error.getErrorId() == OpenPgpError.INCOMPATIBLE_API_VERSIONS) {
            callback.onOpenPgpProviderError(OpenPgpProviderError.VersionIncompatible);
            setOpenPgpProviderState(OpenPgpProviderState.UNCONFIGURED);
        } else {
            callback.onOpenPgpProviderError(OpenPgpProviderError.ConnectionFailed);
            setOpenPgpProviderState(OpenPgpProviderState.ERROR);
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

    public OpenPgpProviderState getOpenPgpProviderState() {
        return openPgpProviderState;
    }

    public enum OpenPgpProviderState {
        UNCONFIGURED,
        UNINITIALIZED,
        LOST_CONNECTION,
        ERROR,
        OK
    }

    public enum OpenPgpProviderError {
        ConnectionFailed, VersionIncompatible, UserInteractionRequired
    }

    public interface OpenPgpApiManagerCallback {
        void launchUserInteractionPendingIntent(PendingIntent pendingIntent);
        void onOpenPgpProviderStatusChanged();
        void onOpenPgpProviderError(OpenPgpProviderError error);
    }
}
