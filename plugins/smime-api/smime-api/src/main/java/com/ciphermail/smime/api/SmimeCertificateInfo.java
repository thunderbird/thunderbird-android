package com.ciphermail.smime.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Certificate availability info for a single recipient email address.
 * Returned by ACTION_GET_CERTIFICATES so the mail client can show per-recipient
 * lock icons in the compose screen.
 */
public class SmimeCertificateInfo implements Parcelable {

    public static final int PARCELABLE_VERSION = 1;

    /** Email address this record describes. */
    public final String email;
    /** True if CipherMail has a usable (non-expired, non-revoked) certificate for this address. */
    public final boolean hasValidCertificate;
    /** Human-readable Subject DN, or null if no certificate is available. */
    public final String subjectDn;

    public SmimeCertificateInfo(String email, boolean hasValidCertificate, String subjectDn) {
        this.email = email;
        this.hasValidCertificate = hasValidCertificate;
        this.subjectDn = subjectDn;
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(PARCELABLE_VERSION);
        int sizePosition = dest.dataPosition();
        dest.writeInt(0);
        int startPosition = dest.dataPosition();
        // version 1
        dest.writeString(email);
        dest.writeInt(hasValidCertificate ? 1 : 0);
        dest.writeString(subjectDn);
        int parcelableSize = dest.dataPosition() - startPosition;
        dest.setDataPosition(sizePosition);
        dest.writeInt(parcelableSize);
        dest.setDataPosition(startPosition + parcelableSize);
    }

    public static final Creator<SmimeCertificateInfo> CREATOR = new Creator<SmimeCertificateInfo>() {
        @Override
        public SmimeCertificateInfo createFromParcel(Parcel source) {
            int version = source.readInt();
            int parcelableSize = source.readInt();
            int startPosition = source.dataPosition();

            String email = source.readString();
            boolean hasValidCertificate = source.readInt() == 1;
            String subjectDn = source.readString();

            source.setDataPosition(startPosition + parcelableSize);
            return new SmimeCertificateInfo(email, hasValidCertificate, subjectDn);
        }

        @Override
        public SmimeCertificateInfo[] newArray(int size) {
            return new SmimeCertificateInfo[size];
        }
    };
}
