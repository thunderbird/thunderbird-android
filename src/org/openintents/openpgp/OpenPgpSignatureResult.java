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
    public static final int SIGNATURE_UNKNOWN_PUB_KEY = 2;
    // successfully verified signature, but with untrusted public key
    public static final int SIGNATURE_SUCCESS_UNTRUSTED = 3;

    int signatureStatus;
    String signatureUserId;
    boolean signatureOnly;

    public int getSignatureStatus() {
        return signatureStatus;
    }

    public String getSignatureUserId() {
        return signatureUserId;
    }

    public boolean isSignatureOnly() {
        return signatureOnly;
    }

    public OpenPgpSignatureResult() {

    }

    public OpenPgpSignatureResult(int signatureStatus, String signatureUserId, boolean signatureOnly) {
        this.signatureStatus = signatureStatus;
        this.signatureUserId = signatureUserId;
        this.signatureOnly = signatureOnly;
    }

    public OpenPgpSignatureResult(OpenPgpSignatureResult b) {
        this.signatureStatus = b.signatureStatus;
        this.signatureUserId = b.signatureUserId;
        this.signatureOnly = b.signatureOnly;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(signatureStatus);
        dest.writeString(signatureUserId);
        dest.writeByte((byte) (signatureOnly ? 1 : 0));
    }

    public static final Creator<OpenPgpSignatureResult> CREATOR = new Creator<OpenPgpSignatureResult>() {
        public OpenPgpSignatureResult createFromParcel(final Parcel source) {
            OpenPgpSignatureResult vr = new OpenPgpSignatureResult();
            vr.signatureStatus = source.readInt();
            vr.signatureUserId = source.readString();
            vr.signatureOnly = source.readByte() == 1;
            return vr;
        }

        public OpenPgpSignatureResult[] newArray(final int size) {
            return new OpenPgpSignatureResult[size];
        }
    };

    @Override
    public String toString() {
        String out = new String();
        out += "\nsignatureStatus: " + signatureStatus;
        out += "\nsignatureUserId: " + signatureUserId;
        out += "\nsignatureOnly: " + signatureOnly;
        return out;
    }

}
