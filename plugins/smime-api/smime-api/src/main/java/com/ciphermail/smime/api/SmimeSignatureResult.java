package com.ciphermail.smime.api;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

/**
 * Signature verification result returned alongside
 * {@code ACTION_DECRYPT_VERIFY}.
 *
 * <p>The {@link #getResult()} code summarises the cryptographic outcome and
 * the trust state of the signer certificate. {@link #getSignerEmail()} and
 * {@link #getSignerSubjectDn()} carry identity information extracted from
 * the signer certificate for UI display; both may be {@code null} when no
 * signature is present.</p>
 *
 * <p>Clients should treat unknown result codes as
 * {@link #RESULT_INVALID_SIGNATURE} to fail safe.</p>
 */
public class SmimeSignatureResult implements Parcelable {

    public static final int PARCELABLE_VERSION = 1;

    /** Message was not signed. */
    public static final int RESULT_NO_SIGNATURE = -1;
    /** Signature is cryptographically invalid. */
    public static final int RESULT_INVALID_SIGNATURE = 0;
    /** Valid signature; certificate chain traces to a trusted root. */
    public static final int RESULT_VALID_TRUSTED = 1;
    /** Valid signature; certificate is not trusted (unknown CA, self-signed, etc.). */
    public static final int RESULT_VALID_UNTRUSTED = 2;
    /** Signer certificate could not be found. */
    public static final int RESULT_CERT_MISSING = 3;
    /** Signer certificate has expired. */
    public static final int RESULT_CERT_EXPIRED = 4;
    /** Signer certificate has been revoked. */
    public static final int RESULT_CERT_REVOKED = 5;

    private final int result;
    /** Email address from the signer's certificate SubjectAltName / Subject CN. */
    @Nullable private final String signerEmail;
    /** Human-readable Subject DN of the signer's certificate. */
    @Nullable private final String signerSubjectDn;

    public SmimeSignatureResult(int result, @Nullable String signerEmail, @Nullable String signerSubjectDn) {
        this.result = result;
        this.signerEmail = signerEmail;
        this.signerSubjectDn = signerSubjectDn;
    }

    public int getResult() { return result; }

    @Nullable
    public String getSignerEmail() { return signerEmail; }

    @Nullable
    public String getSignerSubjectDn() { return signerSubjectDn; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(PARCELABLE_VERSION);
        int sizePosition = dest.dataPosition();
        dest.writeInt(0);
        int startPosition = dest.dataPosition();
        // version 1
        dest.writeInt(result);
        dest.writeString(signerEmail);
        dest.writeString(signerSubjectDn);
        int parcelableSize = dest.dataPosition() - startPosition;
        dest.setDataPosition(sizePosition);
        dest.writeInt(parcelableSize);
        dest.setDataPosition(startPosition + parcelableSize);
    }

    public static final Creator<SmimeSignatureResult> CREATOR = new Creator<SmimeSignatureResult>() {
        @Override
        public SmimeSignatureResult createFromParcel(Parcel source) {
            int version = source.readInt();
            int parcelableSize = source.readInt();
            int startPosition = source.dataPosition();

            int result = source.readInt();
            String signerEmail = source.readString();
            String signerSubjectDn = source.readString();

            source.setDataPosition(startPosition + parcelableSize);
            return new SmimeSignatureResult(result, signerEmail, signerSubjectDn);
        }

        @Override
        public SmimeSignatureResult[] newArray(int size) {
            return new SmimeSignatureResult[size];
        }
    };
}
