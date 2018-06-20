package com.fsck.k9.message;


import java.io.InputStream;

import android.app.PendingIntent;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import timber.log.Timber;


public class AutocryptStatusInteractor {
    private static final AutocryptStatusInteractor INSTANCE = new AutocryptStatusInteractor();

    public static AutocryptStatusInteractor getInstance() {
        return INSTANCE;
    }


    @WorkerThread
    public RecipientAutocryptStatus retrieveCryptoProviderRecipientStatus(
            OpenPgpApi openPgpApi, String[] recipientAddresses) {
        Intent intent = new Intent(OpenPgpApi.ACTION_QUERY_AUTOCRYPT_STATUS);
        intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, recipientAddresses);

        Intent result = openPgpApi.executeApi(intent, (InputStream) null, null);

        switch (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            case OpenPgpApi.RESULT_CODE_SUCCESS:
                RecipientAutocryptStatusType type = getRecipientAutocryptStatusFromIntent(result);
                PendingIntent pendingIntent = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                return new RecipientAutocryptStatus(type, pendingIntent);

            case OpenPgpApi.RESULT_CODE_ERROR:
                OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                if (error != null) {
                    Timber.w("OpenPGP API Error #%s: %s", error.getErrorId(), error.getMessage());
                } else {
                    Timber.w("OpenPGP API Unknown Error");
                }
                return new RecipientAutocryptStatus(RecipientAutocryptStatusType.ERROR, null);
            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED:
                // should never happen, so treat as error!
            default:
                return new RecipientAutocryptStatus(RecipientAutocryptStatusType.ERROR, null);
        }
    }

    @Nullable
    private RecipientAutocryptStatusType getRecipientAutocryptStatusFromIntent(Intent result) {
        boolean allKeysConfirmed = result.getBooleanExtra(OpenPgpApi.RESULT_KEYS_CONFIRMED, false);
        int autocryptStatus =
                result.getIntExtra(OpenPgpApi.RESULT_AUTOCRYPT_STATUS, OpenPgpApi.AUTOCRYPT_STATUS_UNAVAILABLE);

        switch (autocryptStatus) {
            case OpenPgpApi.AUTOCRYPT_STATUS_UNAVAILABLE:
                return RecipientAutocryptStatusType.UNAVAILABLE;
            case OpenPgpApi.AUTOCRYPT_STATUS_DISCOURAGE:
                if (allKeysConfirmed) {
                    return RecipientAutocryptStatusType.DISCOURAGE_CONFIRMED;
                } else {
                    return RecipientAutocryptStatusType.DISCOURAGE_UNCONFIRMED;
                }
            case OpenPgpApi.AUTOCRYPT_STATUS_AVAILABLE:
                if (allKeysConfirmed) {
                    return RecipientAutocryptStatusType.AVAILABLE_CONFIRMED;
                } else {
                    return RecipientAutocryptStatusType.AVAILABLE_UNCONFIRMED;
                }
            case OpenPgpApi.AUTOCRYPT_STATUS_MUTUAL:
                if (allKeysConfirmed) {
                    return RecipientAutocryptStatusType.RECOMMENDED_CONFIRMED;
                } else {
                    return RecipientAutocryptStatusType.RECOMMENDED_UNCONFIRMED;
                }
        }

        throw new IllegalStateException("encountered bad autocrypt status number!");
    }

    public static class RecipientAutocryptStatus {
        public final RecipientAutocryptStatusType type;
        public final PendingIntent intent;

        @VisibleForTesting
        public RecipientAutocryptStatus(RecipientAutocryptStatusType type, PendingIntent intent) {
            this.type = type;
            this.intent = intent;
        }

        public boolean hasPendingIntent() {
            return intent != null;
        }
    }

    public enum RecipientAutocryptStatusType {
        NO_RECIPIENTS (false, false, false),
        UNAVAILABLE (false, false, false),
        DISCOURAGE_UNCONFIRMED (true, false, false),
        DISCOURAGE_CONFIRMED (true, true, false),
        AVAILABLE_UNCONFIRMED (true, false, false),
        AVAILABLE_CONFIRMED (true, true, false),
        RECOMMENDED_UNCONFIRMED (true, false, true),
        RECOMMENDED_CONFIRMED (true, true, true),
        ERROR (false, false, false);

        private final boolean canEncrypt;
        private final boolean isConfirmed;
        private final boolean isMutual;

        RecipientAutocryptStatusType(boolean canEncrypt, boolean isConfirmed, boolean isMutual) {
            this.canEncrypt = canEncrypt;
            this.isConfirmed = isConfirmed;
            this.isMutual = isMutual;
        }

        public boolean canEncrypt() {
            return canEncrypt;
        }

        public boolean isConfirmed() {
            return isConfirmed;
        }

        public boolean isMutual() {
            return isMutual;
        }
    }
}
