package com.ciphermail.smime.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Decryption outcome returned alongside {@code ACTION_DECRYPT_VERIFY}.
 *
 * <p>Distinguishes "this message was actually encrypted and we decrypted it"
 * from "this message was already plaintext" so the client can label the
 * message appropriately. Signature state is reported separately by
 * {@link SmimeSignatureResult}.</p>
 */
public class SmimeDecryptionResult implements Parcelable {

    public static final int PARCELABLE_VERSION = 1;

    /** Message was not encrypted (plain or signed-only). */
    public static final int RESULT_NOT_ENCRYPTED = -1;
    /** Message was encrypted and successfully decrypted. */
    public static final int RESULT_ENCRYPTED = 1;

    public final int result;

    public SmimeDecryptionResult(int result) {
        this.result = result;
    }

    public int getResult() {
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(PARCELABLE_VERSION);
        int sizePosition = dest.dataPosition();
        dest.writeInt(0);
        int startPosition = dest.dataPosition();
        // version 1
        dest.writeInt(result);
        int parcelableSize = dest.dataPosition() - startPosition;
        dest.setDataPosition(sizePosition);
        dest.writeInt(parcelableSize);
        dest.setDataPosition(startPosition + parcelableSize);
    }

    public static final Creator<SmimeDecryptionResult> CREATOR = new Creator<SmimeDecryptionResult>() {
        @Override
        public SmimeDecryptionResult createFromParcel(Parcel source) {
            int version = source.readInt();
            int parcelableSize = source.readInt();
            int startPosition = source.dataPosition();

            int result = source.readInt();

            source.setDataPosition(startPosition + parcelableSize);
            return new SmimeDecryptionResult(result);
        }

        @Override
        public SmimeDecryptionResult[] newArray(int size) {
            return new SmimeDecryptionResult[size];
        }
    };
}
