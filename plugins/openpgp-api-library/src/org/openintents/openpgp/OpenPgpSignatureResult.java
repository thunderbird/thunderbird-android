/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.openintents.openpgp.util.OpenPgpUtils;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Parcelable versioning has been copied from Dashclock Widget
 * https://code.google.com/p/dashclock/source/browse/api/src/main/java/com/google/android/apps/dashclock/api/ExtensionData.java
 */
public class OpenPgpSignatureResult implements Parcelable {
    /**
     * Since there might be a case where new versions of the client using the library getting
     * old versions of the protocol (and thus old versions of this class), we need a versioning
     * system for the parcels sent between the clients and the providers.
     */
    public static final int PARCELABLE_VERSION = 2;

    // generic error on signature verification
    public static final int SIGNATURE_ERROR = 0;
    // successfully verified signature, with certified key
    public static final int SIGNATURE_SUCCESS_CERTIFIED = 1;
    // no key was found for this signature verification
    public static final int SIGNATURE_KEY_MISSING = 2;
    // successfully verified signature, but with uncertified key
    public static final int SIGNATURE_SUCCESS_UNCERTIFIED = 3;
    // key has been revoked
    public static final int SIGNATURE_KEY_REVOKED = 4;
    // key is expired
    public static final int SIGNATURE_KEY_EXPIRED = 5;

    int status;
    boolean signatureOnly;
    String primaryUserId;
    ArrayList<String> userIds;
    long keyId;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isSignatureOnly() {
        return signatureOnly;
    }

    public void setSignatureOnly(boolean signatureOnly) {
        this.signatureOnly = signatureOnly;
    }

    public String getPrimaryUserId() {
        return primaryUserId;
    }

    public void setPrimaryUserId(String primaryUserId) {
        this.primaryUserId = primaryUserId;
    }

    public ArrayList<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(ArrayList<String> userIds) {
        this.userIds = userIds;
    }

    public long getKeyId() {
        return keyId;
    }

    public void setKeyId(long keyId) {
        this.keyId = keyId;
    }

    public OpenPgpSignatureResult() {

    }

    public OpenPgpSignatureResult(int signatureStatus, String signatureUserId,
                                  boolean signatureOnly, long keyId, ArrayList<String> userIds) {
        this.status = signatureStatus;
        this.signatureOnly = signatureOnly;
        this.primaryUserId = signatureUserId;
        this.keyId = keyId;
        this.userIds = userIds;
    }

    public OpenPgpSignatureResult(OpenPgpSignatureResult b) {
        this.status = b.status;
        this.primaryUserId = b.primaryUserId;
        this.signatureOnly = b.signatureOnly;
        this.keyId = b.keyId;
        this.userIds = b.userIds;
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
        dest.writeInt(status);
        dest.writeByte((byte) (signatureOnly ? 1 : 0));
        dest.writeString(primaryUserId);
        dest.writeLong(keyId);
        // version 2
        dest.writeStringList(userIds);
        // Go back and write the size
        int parcelableSize = dest.dataPosition() - startPosition;
        dest.setDataPosition(sizePosition);
        dest.writeInt(parcelableSize);
        dest.setDataPosition(startPosition + parcelableSize);
    }

    public static final Creator<OpenPgpSignatureResult> CREATOR = new Creator<OpenPgpSignatureResult>() {
        public OpenPgpSignatureResult createFromParcel(final Parcel source) {
            int parcelableVersion = source.readInt();
            int parcelableSize = source.readInt();
            int startPosition = source.dataPosition();

            OpenPgpSignatureResult vr = new OpenPgpSignatureResult();
            vr.status = source.readInt();
            vr.signatureOnly = source.readByte() == 1;
            vr.primaryUserId = source.readString();
            vr.keyId = source.readLong();
            vr.userIds = new ArrayList<String>();
            source.readStringList(vr.userIds);

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
        String out = "\nstatus: " + status;
        out += "\nprimaryUserId: " + primaryUserId;
        out += "\nuserIds: " + userIds;
        out += "\nsignatureOnly: " + signatureOnly;
        out += "\nkeyId: " + OpenPgpUtils.convertKeyIdToHex(keyId);
        return out;
    }

}
