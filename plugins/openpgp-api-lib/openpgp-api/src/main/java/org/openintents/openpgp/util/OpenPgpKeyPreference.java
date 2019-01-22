/*
 * Copyright (C) 2018 The K-9 Dog Walkers
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.TypedArray;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import android.text.format.DateUtils;
import android.util.AttributeSet;

import org.openintents.openpgp.OpenPgpApiManager;
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpApiManagerCallback;
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpProviderError;
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpProviderState;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.R;
import org.openintents.openpgp.util.OpenPgpApi.IOpenPgpCallback;
import org.openintents.openpgp.util.OpenPgpUtils.UserId;
import timber.log.Timber;


public class OpenPgpKeyPreference extends Preference implements OpenPgpApiManagerCallback {
    private long keyId;
    private String defaultUserId;
    private boolean showAutocryptHint;
    private OpenPgpApiManager openPgpApiManager;
    private Intent cachedActivityResultData;
    private Fragment intentSenderFragment;

    private PendingIntent pendingIntentSelectKey;
    private boolean pendingIntentRunImmediately;

    private String keyPrimaryUserId;
    private long keyCreationTime;

    private static final int REQUEST_CODE_API_MANAGER = 9998;
    private static final int REQUEST_CODE_KEY_PREFERENCE = 9999;

    private static final int NO_KEY = 0;

    public OpenPgpKeyPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setOpenPgpProvider(OpenPgpApiManager openPgpApiManager, String openPgpProvider) {
        this.openPgpApiManager = openPgpApiManager;
        this.openPgpApiManager.setOpenPgpProvider(openPgpProvider, this);
        refreshTitleAndSummary();
    }

    public void setIntentSenderFragment(Fragment fragment) {
        intentSenderFragment = fragment;
    }

    public void setDefaultUserId(String userId) {
        defaultUserId = userId;
    }

    public void setShowAutocryptHint(boolean showAutocryptHint) {
        this.showAutocryptHint = showAutocryptHint;
    }

    @Override
    protected void onClick() {
        switch (openPgpApiManager.getOpenPgpProviderState()) {
            // The GET_SIGN_KEY action is special, in that it can be used as an implicit registration
            // to the API. Therefore, we can ignore the UI_REQUIRED here. If it comes up regardless,
            // it will also work as a regular pending intent.
            case UI_REQUIRED:
            case OK: {
                apiGetOrStartPendingIntent();
                break;
            }
            default: {
                refreshTitleAndSummary();
                openPgpApiManager.refreshConnection();
                break;
            }
        }
    }

    @Override
    public void onOpenPgpProviderStatusChanged() {
        if (openPgpApiManager.getOpenPgpProviderState() == OpenPgpProviderState.OK) {
            apiRetrievePendingIntentAndKeyInfo();
        } else {
            pendingIntentSelectKey = null;
            pendingIntentRunImmediately = false;
            cachedActivityResultData = null;
            refreshTitleAndSummary();
        }
    }

    @Override
    public void onOpenPgpProviderError(OpenPgpProviderError error) {
        if (error == OpenPgpProviderError.ConnectionLost) {
            openPgpApiManager.refreshConnection();
        }
    }

    private void apiRetrievePendingIntentAndKeyInfo() {
        Intent data;
        if (cachedActivityResultData != null) {
            data = cachedActivityResultData;
            cachedActivityResultData = null;
        } else {
            data = new Intent();
        }
        apiRetrievePendingIntentAndKeyInfo(data);
    }

    private void apiRetrievePendingIntentAndKeyInfo(Intent data) {
        data.setAction(OpenPgpApi.ACTION_GET_SIGN_KEY_ID);
        data.putExtra(OpenPgpApi.EXTRA_USER_ID, defaultUserId);
        data.putExtra(OpenPgpApi.EXTRA_PRESELECT_KEY_ID, keyId);
        data.putExtra(OpenPgpApi.EXTRA_SHOW_AUTOCRYPT_HINT, showAutocryptHint);
        OpenPgpApi api = openPgpApiManager.getOpenPgpApi();
        api.executeApiAsync(data, null, null, openPgpCallback);
    }

    private IOpenPgpCallback openPgpCallback = new IOpenPgpCallback() {
        @Override
        public void onReturn(Intent result) {
            int resultCode = result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
            switch (resultCode) {
                case OpenPgpApi.RESULT_CODE_SUCCESS:
                case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED: {
                    PendingIntent pendingIntentSelectKey = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);

                    if (result.hasExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID)) {
                        long keyId = result.getLongExtra(OpenPgpApi.EXTRA_SIGN_KEY_ID, NO_KEY);
                        long keyCreationTime = result.getLongExtra("key_creation_time", 0);
                        String primaryUserId = result.getStringExtra("primary_user_id");

                        updateWidgetData(keyId, primaryUserId, keyCreationTime, pendingIntentSelectKey);
                    } else {
                        updateWidgetData(pendingIntentSelectKey);
                    }

                    break;
                }
                case OpenPgpApi.RESULT_CODE_ERROR: {
                    OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                    Timber.e("RESULT_CODE_ERROR: %s", error.getMessage());

                    break;
                }
            }
        }
    };

    private void apiGetOrStartPendingIntent() {
        if (pendingIntentSelectKey != null) {
            apiStartPendingIntent();
            return;
        }

        pendingIntentRunImmediately = true;
        apiRetrievePendingIntentAndKeyInfo();
    }

    private void apiStartPendingIntent() {
        if (pendingIntentSelectKey == null) {
            Timber.e("Tried to launch pending intent but didn't have any?");
            return;
        }

        try {
            intentSenderFragment
                    .startIntentSenderForResult(pendingIntentSelectKey.getIntentSender(), REQUEST_CODE_KEY_PREFERENCE,
                    null, 0, 0, 0, null);
        } catch (IntentSender.SendIntentException e) {
            Timber.e(e,"Error launching pending intent");
        } finally {
            pendingIntentSelectKey = null;
        }
    }

    private void updateWidgetData(PendingIntent pendingIntentSelectKey) {
        this.keyPrimaryUserId = null;
        this.keyCreationTime = 0;
        this.pendingIntentSelectKey = pendingIntentSelectKey;

        maybeRunPendingIntentImmediately();
        refreshTitleAndSummary();
    }

    private void updateWidgetData(long keyId, String primaryUserId, long keyCreationTime,
            PendingIntent pendingIntentSelectKey) {
        setAndPersist(keyId);
        this.keyPrimaryUserId = primaryUserId;
        this.keyCreationTime = keyCreationTime;
        this.pendingIntentSelectKey = pendingIntentSelectKey;

        callChangeListener(keyId);
        maybeRunPendingIntentImmediately();
        refreshTitleAndSummary();
    }

    private void maybeRunPendingIntentImmediately() {
        if (!pendingIntentRunImmediately) {
            return;
        }

        pendingIntentRunImmediately = false;
        apiStartPendingIntent();
    }

    private void refreshTitleAndSummary() {
        boolean isConfigured = openPgpApiManager != null &&
                openPgpApiManager.getOpenPgpProviderState() != OpenPgpProviderState.UNCONFIGURED;
        setEnabled(isConfigured);

        if (this.keyId == NO_KEY) {
            setTitle(R.string.openpgp_key_title);
            setSummary(R.string.openpgp_no_key_selected);

            return;
        }

        if (this.keyPrimaryUserId != null && this.keyCreationTime != 0) {
            Context context = getContext();

            UserId userId = OpenPgpUtils.splitUserId(keyPrimaryUserId);
            if (userId.email != null) {
                setTitle(context.getString(R.string.openpgp_key_using, userId.email));
            } else if (userId.name != null) {
                setTitle(context.getString(R.string.openpgp_key_using, userId.name));
            } else {
                setTitle(R.string.openpgp_key_using_no_name);
            }

            String creationTimeStr = DateUtils.formatDateTime(context, keyCreationTime,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME |
                            DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_MONTH);
            setSummary(context.getString(R.string.openpgp_key_created, creationTimeStr));
        } else {
            setTitle(R.string.openpgp_key_title);
            setSummary(R.string.openpgp_key_selected);
        }
    }

    /**
     * Public API
     */
    public void setValue(long keyId) {
        setAndPersist(keyId);
        refreshTitleAndSummary();
    }

    /**
     * Public API
     */
    public long getValue() {
        return keyId;
    }

    private void setAndPersist(long newValue) {
        keyId = newValue;
        notifyDependencyChange(shouldDisableDependents());

        // Save to persistent storage (this method will make sure this
        // preference should be persistent, along with other useful checks)
        persistLong(keyId);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        // This preference type's value type is Long, so we read the default
        // value from the attributes as an Integer.
        return (long) a.getInteger(index, NO_KEY);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            // Restore state
            keyId = getPersistedLong(keyId);
            notifyDependencyChange(shouldDisableDependents());
        } else {
            // Set state
            long value = (Long) defaultValue;
            setAndPersist(value);
        }
    }

    @Override
    public boolean shouldDisableDependents() {
        return keyId == NO_KEY || super.shouldDisableDependents();
    }

    public boolean handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_API_MANAGER:
                openPgpApiManager.onUserInteractionResult();
                return true;
            case REQUEST_CODE_KEY_PREFERENCE:
                if (resultCode == Activity.RESULT_OK) {
                    cachedActivityResultData = data;
                    // this might happen early in the lifecycle (e.g. before onResume). if the provider isn't connected
                    // here, apiRetrievePendingIntentAndKeyInfo() will be called as soon as it is.
                    OpenPgpProviderState openPgpProviderState = openPgpApiManager.getOpenPgpProviderState();
                    if (openPgpProviderState == OpenPgpProviderState.OK ||
                            openPgpProviderState == OpenPgpProviderState.UI_REQUIRED) {
                        apiRetrievePendingIntentAndKeyInfo();
                    }
                }
                return true;
        }
        return false;
    }

}
