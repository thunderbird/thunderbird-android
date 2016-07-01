/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

import org.openintents.openpgp.util.OpenPgpUtils;

@SuppressWarnings("unused")
public class OpenPgpSignatureResult implements Parcelable {
    /**
     * Since there might be a case where new versions of the client using the library getting
     * old versions of the protocol (and thus old versions of this class), we need a versioning
     * system for the parcels sent between the clients and the providers.
     */
    public static final int PARCELABLE_VERSION = 3;

    // content not signed
    public static final int RESULT_NO_SIGNATURE = -1;
    // invalid signature!
    public static final int RESULT_INVALID_SIGNATURE = 0;
    // successfully verified signature, with confirmed key
    @Deprecated
    public static final int RESULT_VALID_CONFIRMED = 1;
    public static final int RESULT_VALID_KEY_CONFIRMED = 1;
    // no key was found for this signature verification
    public static final int RESULT_KEY_MISSING = 2;
    // successfully verified signature, but with unconfirmed key
    @Deprecated
    public static final int RESULT_VALID_UNCONFIRMED = 3;
    public static final int RESULT_VALID_KEY_UNCONFIRMED = 3;
    // key has been revoked -> invalid signature!
    public static final int RESULT_INVALID_KEY_REVOKED = 4;
    // key is expired -> invalid signature!
    public static final int RESULT_INVALID_KEY_EXPIRED = 5;
    // insecure cryptographic algorithms/protocol -> invalid signature!
    @Deprecated
    public static final int RESULT_INVALID_INSECURE = 6;
    public static final int RESULT_INVALID_KEY_INSECURE = 6;

    public static final int SENDER_RESULT_NO_SENDER = 0;
    public static final int SENDER_RESULT_UID_CONFIRMED = 1;
    public static final int SENDER_RESULT_UID_UNCONFIRMED = 2;
    public static final int SENDER_RESULT_UID_MISSING = 3;

    private final int result;
    private final long keyId;
    private final String primaryUserId;
    private final ArrayList<String> userIds;
    private final ArrayList<String> confirmedUserIds;
    private final int senderResult;

    private OpenPgpSignatureResult(int signatureStatus, String signatureUserId, long keyId,
            ArrayList<String> userIds, ArrayList<String> confirmedUserIds, int senderResult, Boolean signatureOnly) {
        this.result = signatureStatus;
        this.primaryUserId = signatureUserId;
        this.keyId = keyId;
        this.userIds = userIds;
        this.confirmedUserIds = confirmedUserIds;
        this.senderResult = senderResult;
    }

    private OpenPgpSignatureResult(Parcel source, int version) {
        this.result = source.readInt();
        // we dropped support for signatureOnly, but need to skip the value for compatibility
        source.readByte();
        this.primaryUserId = source.readString();
        this.keyId = source.readLong();

        if (version > 1) {
            this.userIds = source.createStringArrayList();
        } else {
            this.userIds = null;
        }
        if (version > 2) {
            this.senderResult = source.readInt();
            this.confirmedUserIds = source.createStringArrayList();
        } else {
            this.senderResult = SENDER_RESULT_NO_SENDER;
            this.confirmedUserIds = null;
        }
    }

    public int getResult() {
        return result;
    }

    public int getSenderResult() {
        return senderResult;
    }

    public String getPrimaryUserId() {
        return primaryUserId;
    }

    public List<String> getUserIds() {
        return Collections.unmodifiableList(userIds);
    }

    public List<String> getConfirmedUserIds() {
        return Collections.unmodifiableList(confirmedUserIds);
    }

    public long getKeyId() {
        return keyId;
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
        // this is deprecated since version 3. we pass a dummy value for compatibility
        dest.writeByte((byte) 0);
        dest.writeString(primaryUserId);
        dest.writeLong(keyId);
        // version 2
        dest.writeStringList(userIds);
        // version 3
        dest.writeInt(senderResult);
        dest.writeStringList(confirmedUserIds);
        // Go back and write the size
        int parcelableSize = dest.dataPosition() - startPosition;
        dest.setDataPosition(sizePosition);
        dest.writeInt(parcelableSize);
        dest.setDataPosition(startPosition + parcelableSize);
    }

    public static final Creator<OpenPgpSignatureResult> CREATOR = new Creator<OpenPgpSignatureResult>() {
        public OpenPgpSignatureResult createFromParcel(final Parcel source) {
            int version = source.readInt(); // parcelableVersion
            int parcelableSize = source.readInt();
            int startPosition = source.dataPosition();

            OpenPgpSignatureResult vr = new OpenPgpSignatureResult(source, version);

            // skip over all fields added in future versions of this parcel
            source.setDataPosition(startPosition + parcelableSize);

            return vr;
        }

        public OpenPgpSignatureResult[] newArray(final int size) {
            return new OpenPgpSignatureResult[size];
        }
    };

    @Override
    public String toString() {
        String out = "\nresult: " + result;
        out += "\nprimaryUserId: " + primaryUserId;
        out += "\nuserIds: " + userIds;
        out += "\nkeyId: " + OpenPgpUtils.convertKeyIdToHex(keyId);
        return out;
    }

    public static OpenPgpSignatureResult createWithValidSignature(int signatureStatus, String primaryUserId,
            long keyId, ArrayList<String> userIds, ArrayList<String> confirmedUserIds, int senderStatus) {
        if (signatureStatus == RESULT_NO_SIGNATURE || signatureStatus == RESULT_KEY_MISSING ||
                signatureStatus == RESULT_INVALID_SIGNATURE) {
            throw new IllegalArgumentException("can only use this method for valid types of signatures");
        }
        return new OpenPgpSignatureResult(
                signatureStatus, primaryUserId, keyId, userIds, confirmedUserIds, senderStatus, null);
    }

    public static OpenPgpSignatureResult createWithNoSignature() {
        return new OpenPgpSignatureResult(RESULT_NO_SIGNATURE, null, 0L, null, null, 0, null);
    }

    public static OpenPgpSignatureResult createWithKeyMissing(long keyId) {
        return new OpenPgpSignatureResult(RESULT_KEY_MISSING, null, keyId, null, null, 0, null);
    }

    public static OpenPgpSignatureResult createWithInvalidSignature() {
        return new OpenPgpSignatureResult(RESULT_INVALID_SIGNATURE, null, 0L, null, null, 0, null);
    }

    @Deprecated
    public OpenPgpSignatureResult withSignatureOnlyFlag(boolean signatureOnly) {
        return new OpenPgpSignatureResult(
                result, primaryUserId, keyId, userIds, confirmedUserIds, senderResult, signatureOnly);
    }
}
