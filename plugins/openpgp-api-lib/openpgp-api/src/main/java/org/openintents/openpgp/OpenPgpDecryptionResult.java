/*
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

package org.openintents.openpgp;

import android.os.Parcel;
import android.os.Parcelable;

public class OpenPgpDecryptionResult implements Parcelable {
    /**
     * Since there might be a case where new versions of the client using the library getting
     * old versions of the protocol (and thus old versions of this class), we need a versioning
     * system for the parcels sent between the clients and the providers.
     */
    public static final int PARCELABLE_VERSION = 2;

    // content not encrypted
    public static final int RESULT_NOT_ENCRYPTED = -1;
    // insecure!
    public static final int RESULT_INSECURE = 0;
    // encrypted
    public static final int RESULT_ENCRYPTED = 1;

    public final int result;
    public final byte[] sessionKey;
    public final byte[] decryptedSessionKey;

    public int getResult() {
        return result;
    }

    public OpenPgpDecryptionResult(int result) {
        this.result = result;
        this.sessionKey = null;
        this.decryptedSessionKey = null;
    }

    public OpenPgpDecryptionResult(int result, byte[] sessionKey, byte[] decryptedSessionKey) {
        this.result = result;
        if ((sessionKey == null) != (decryptedSessionKey == null)) {
            throw new AssertionError("sessionkey must be null iff decryptedSessionKey is null");
        }
        this.sessionKey = sessionKey;
        this.decryptedSessionKey = decryptedSessionKey;
    }

    public OpenPgpDecryptionResult(OpenPgpDecryptionResult b) {
        this.result = b.result;
        this.sessionKey = b.sessionKey;
        this.decryptedSessionKey = b.decryptedSessionKey;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        /**
         * NOTE: When adding fields in the process of updating this API, make sure to bump
         * {@link #PARCELABLE_VERSION}.
         */
        dest.writeInt(PARCELABLE_VERSION);
        // Inject a placeholder that will store the parcel size from this point on
        // (not including the size itself).
        int sizePosition = dest.dataPosition();
        dest.writeInt(0);
        int startPosition = dest.dataPosition();
        // version 1
        dest.writeInt(result);
        // version 2
        dest.writeByteArray(sessionKey);
        dest.writeByteArray(decryptedSessionKey);
        // Go back and write the size
        int parcelableSize = dest.dataPosition() - startPosition;
        dest.setDataPosition(sizePosition);
        dest.writeInt(parcelableSize);
        dest.setDataPosition(startPosition + parcelableSize);
    }

    public static final Creator<OpenPgpDecryptionResult> CREATOR = new Creator<OpenPgpDecryptionResult>() {
        public OpenPgpDecryptionResult createFromParcel(final Parcel source) {
            int version = source.readInt(); // parcelableVersion
            int parcelableSize = source.readInt();
            int startPosition = source.dataPosition();

            int result = source.readInt();
            byte[] sessionKey = version > 1 ? source.createByteArray() : null;
            byte[] decryptedSessionKey = version > 1 ? source.createByteArray() : null;

            OpenPgpDecryptionResult vr = new OpenPgpDecryptionResult(result, sessionKey, decryptedSessionKey);

            // skip over all fields added in future versions of this parcel
            source.setDataPosition(startPosition + parcelableSize);

            return vr;
        }

        public OpenPgpDecryptionResult[] newArray(final int size) {
            return new OpenPgpDecryptionResult[size];
        }
    };

    @Override
    public String toString() {
        return "\nresult: " + result;
    }

}
