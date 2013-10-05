/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

public class OpenPgpSignatureResult implements Parcelable {
    // generic error on signature verification
    public static final int SIGNATURE_ERROR = 0;
    // successfully verified signature, with trusted public key
    public static final int SIGNATURE_SUCCESS_TRUSTED = 1;
    // no public key was found for this signature verification
    // you can retrieve the key with
    // getKeys(new String[] {String.valueOf(signatureResult.getKeyId)}, true, callback)
    public static final int SIGNATURE_UNKNOWN_PUB_KEY = 2;
    // successfully verified signature, but with untrusted public key
    public static final int SIGNATURE_SUCCESS_UNTRUSTED = 3;

    int status;
    boolean signatureOnly;
    String userId;
    long keyId;

    public int getStatus() {
        return status;
    }

    public boolean isSignatureOnly() {
        return signatureOnly;
    }

    public String getUserId() {
        return userId;
    }

    public long getKeyId() {
        return keyId;
    }

    public OpenPgpSignatureResult() {

    }

    public OpenPgpSignatureResult(int signatureStatus, String signatureUserId,
            boolean signatureOnly, long keyId) {
        this.status = signatureStatus;
        this.signatureOnly = signatureOnly;
        this.userId = signatureUserId;
        this.keyId = keyId;
    }

    public OpenPgpSignatureResult(OpenPgpSignatureResult b) {
        this.status = b.status;
        this.userId = b.userId;
        this.signatureOnly = b.signatureOnly;
        this.keyId = b.keyId;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(status);
        dest.writeByte((byte) (signatureOnly ? 1 : 0));
        dest.writeString(userId);
        dest.writeLong(keyId);
    }

    public static final Creator<OpenPgpSignatureResult> CREATOR = new Creator<OpenPgpSignatureResult>() {
        public OpenPgpSignatureResult createFromParcel(final Parcel source) {
            OpenPgpSignatureResult vr = new OpenPgpSignatureResult();
            vr.status = source.readInt();
            vr.signatureOnly = source.readByte() == 1;
            vr.userId = source.readString();
            vr.keyId = source.readLong();
            return vr;
        }

        public OpenPgpSignatureResult[] newArray(final int size) {
            return new OpenPgpSignatureResult[size];
        }
    };

    @Override
    public String toString() {
        String out = new String();
        out += "\nstatus: " + status;
        out += "\nuserId: " + userId;
        out += "\nsignatureOnly: " + signatureOnly;
        out += "\nkeyId: " + keyId;
        return out;
    }

}
