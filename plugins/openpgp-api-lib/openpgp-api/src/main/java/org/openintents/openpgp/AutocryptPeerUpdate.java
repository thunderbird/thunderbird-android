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


import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;


@SuppressWarnings("unused")
public class AutocryptPeerUpdate implements Parcelable {
    /**
     * Since there might be a case where new versions of the client using the library getting
     * old versions of the protocol (and thus old versions of this class), we need a versioning
     * system for the parcels sent between the clients and the providers.
     */
    private static final int PARCELABLE_VERSION = 1;


    private final byte[] keyData;
    private final Date effectiveDate;
    private final PreferEncrypt preferEncrypt;


    private AutocryptPeerUpdate(byte[] keyData, Date effectiveDate, PreferEncrypt preferEncrypt) {
        this.keyData = keyData;
        this.effectiveDate = effectiveDate;
        this.preferEncrypt = preferEncrypt;
    }

    private AutocryptPeerUpdate(Parcel source, int version) {
        this.keyData = source.createByteArray();
        this.effectiveDate = source.readInt() != 0 ? new Date(source.readLong()) : null;
        this.preferEncrypt = PreferEncrypt.values()[source.readInt()];
    }


    public static AutocryptPeerUpdate createAutocryptPeerUpdate(byte[] keyData, Date timestamp) {
        return new AutocryptPeerUpdate(keyData, timestamp, PreferEncrypt.NOPREFERENCE);
    }

    public byte[] getKeyData() {
        return keyData;
    }

    public boolean hasKeyData() {
        return keyData != null;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public PreferEncrypt getPreferEncrypt() {
        return preferEncrypt;
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
        dest.writeByteArray(keyData);
        if (effectiveDate != null) {
            dest.writeInt(1);
            dest.writeLong(effectiveDate.getTime());
        } else {
            dest.writeInt(0);
        }

        dest.writeInt(preferEncrypt.ordinal());

        // Go back and write the size
        int parcelableSize = dest.dataPosition() - startPosition;
        dest.setDataPosition(sizePosition);
        dest.writeInt(parcelableSize);
        dest.setDataPosition(startPosition + parcelableSize);
    }

    public static final Creator<AutocryptPeerUpdate> CREATOR = new Creator<AutocryptPeerUpdate>() {
        public AutocryptPeerUpdate createFromParcel(final Parcel source) {
            int version = source.readInt(); // parcelableVersion
            int parcelableSize = source.readInt();
            int startPosition = source.dataPosition();

            AutocryptPeerUpdate vr = new AutocryptPeerUpdate(source, version);

            // skip over all fields added in future versions of this parcel
            source.setDataPosition(startPosition + parcelableSize);

            return vr;
        }

        public AutocryptPeerUpdate[] newArray(final int size) {
            return new AutocryptPeerUpdate[size];
        }
    };

    public enum PreferEncrypt {
        NOPREFERENCE, MUTUAL;
    }
}
