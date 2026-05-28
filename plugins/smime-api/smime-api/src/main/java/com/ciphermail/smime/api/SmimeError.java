package com.ciphermail.smime.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Error result returned from the S/MIME service when {@code RESULT_CODE} is
 * {@code SmimeApi.RESULT_CODE_ERROR}.
 *
 * <p>The integer error identifier ({@link #getErrorId()}) is stable across
 * versions and can be used to drive client-side messaging without parsing
 * the human-readable {@link #getMessage()} string.</p>
 *
 * <p>Construction is asymmetric on purpose: the service produces these and
 * the client only reads them. New error codes may be added in future API
 * versions; clients should treat unknown ids as equivalent to
 * {@link #GENERIC_ERROR}.</p>
 */
public class SmimeError implements Parcelable {

    public static final int PARCELABLE_VERSION = 1;

    /**
     * Error that originated in the client-side helper (e.g. failed to set up
     * the ParcelFileDescriptor pipe) rather than in the provider service.
     */
    public static final int CLIENT_SIDE_ERROR = -1;

    /** Unspecified failure in the provider. Inspect {@link #getMessage()}. */
    public static final int GENERIC_ERROR = 0;

    /**
     * The caller supplied an {@code EXTRA_API_VERSION} that the provider
     * cannot serve. The client should not retry without code changes.
     */
    public static final int INCOMPATIBLE_API_VERSIONS = 1;

    /**
     * One or more recipient addresses (passed via {@code EXTRA_USER_IDS}) have
     * no usable certificate. Specific to {@code ACTION_SIGN_AND_ENCRYPT}.
     */
    public static final int NO_CERTIFICATE_FOR_RECIPIENT = 2;

    /**
     * Provider's keystore is locked and the user did not complete the
     * passphrase dialog. Clients normally see
     * {@code RESULT_CODE_USER_INTERACTION_REQUIRED} instead of this code;
     * this code is only set when interaction was offered and declined.
     */
    public static final int KEYSTORE_LOCKED = 3;

    private final int errorId;
    private final String message;

    public SmimeError(int errorId, String message) {
        this.errorId = errorId;
        this.message = message;
    }

    public int getErrorId() { return errorId; }
    public String getMessage() { return message; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(PARCELABLE_VERSION);
        int sizePosition = dest.dataPosition();
        dest.writeInt(0);
        int startPosition = dest.dataPosition();
        // version 1
        dest.writeInt(errorId);
        dest.writeString(message);
        int parcelableSize = dest.dataPosition() - startPosition;
        dest.setDataPosition(sizePosition);
        dest.writeInt(parcelableSize);
        dest.setDataPosition(startPosition + parcelableSize);
    }

    public static final Creator<SmimeError> CREATOR = new Creator<SmimeError>() {
        @Override
        public SmimeError createFromParcel(Parcel source) {
            int version = source.readInt();
            int parcelableSize = source.readInt();
            int startPosition = source.dataPosition();

            int errorId = source.readInt();
            String message = source.readString();

            source.setDataPosition(startPosition + parcelableSize);
            return new SmimeError(errorId, message);
        }

        @Override
        public SmimeError[] newArray(int size) {
            return new SmimeError[size];
        }
    };
}
