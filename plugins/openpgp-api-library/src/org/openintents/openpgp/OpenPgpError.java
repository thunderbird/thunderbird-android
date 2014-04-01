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

/**
 * Parcelable versioning has been copied from Dashclock Widget
 * https://code.google.com/p/dashclock/source/browse/api/src/main/java/com/google/android/apps/dashclock/api/ExtensionData.java
 */
public class OpenPgpError implements Parcelable {
    /**
     * Since there might be a case where new versions of the client using the library getting
     * old versions of the protocol (and thus old versions of this class), we need a versioning
     * system for the parcels sent between the clients and the providers.
     */
    public static final int PARCELABLE_VERSION = 1;

    // possible values for errorId
    public static final int CLIENT_SIDE_ERROR = -1;
    public static final int GENERIC_ERROR = 0;
    public static final int INCOMPATIBLE_API_VERSIONS = 1;
    public static final int NO_OR_WRONG_PASSPHRASE = 2;
    public static final int NO_USER_IDS = 3;

    int errorId;
    String message;

    public OpenPgpError() {
    }

    public OpenPgpError(int errorId, String message) {
        this.errorId = errorId;
        this.message = message;
    }

    public OpenPgpError(OpenPgpError b) {
        this.errorId = b.errorId;
        this.message = b.message;
    }

    public int getErrorId() {
        return errorId;
    }

    public void setErrorId(int errorId) {
        this.errorId = errorId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
        dest.writeInt(errorId);
        dest.writeString(message);
        // Go back and write the size
        int parcelableSize = dest.dataPosition() - startPosition;
        dest.setDataPosition(sizePosition);
        dest.writeInt(parcelableSize);
        dest.setDataPosition(startPosition + parcelableSize);
    }

    public static final Creator<OpenPgpError> CREATOR = new Creator<OpenPgpError>() {
        public OpenPgpError createFromParcel(final Parcel source) {
            int parcelableVersion = source.readInt();
            int parcelableSize = source.readInt();
            int startPosition = source.dataPosition();

            OpenPgpError error = new OpenPgpError();
            error.errorId = source.readInt();
            error.message = source.readString();

            // skip over all fields added in future versions of this parcel
            source.setDataPosition(startPosition + parcelableSize);

            return error;
        }

        public OpenPgpError[] newArray(final int size) {
            return new OpenPgpError[size];
        }
    };
}
